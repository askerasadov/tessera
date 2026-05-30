package io.lightine.tessera.mrz.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.common.util.concurrent.ListenableFuture
import io.lightine.tessera.telemetry.NoOpTelemetrySink
import io.lightine.tessera.telemetry.TelemetrySink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The Android owns-the-camera-session scanner: runs CameraX internally and streams a [MrzScanResult] per
 * analysed frame, so the consumer never touches `ImageAnalysis` / `bindToLifecycle`
 * ([ADR-020](https://github.com/lightine-io/tessera/blob/main/docs/decisions/0020-camera-reading-architecture.md)).
 * It binds an [ImageAnalysis] use case (back-pressure [STRATEGY_KEEP_ONLY_LATEST][ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST]
 * — only the newest frame is analysed; intermediate frames are dropped, which is exactly what a live MRZ
 * scan wants) to the supplied [lifecycleOwner], pipes each `ImageProxy` through [MrzFrameAnalyzer.scan],
 * and closes the proxy once analysed (CameraX cannot deliver the next frame until the current one is
 * closed — memory hygiene).
 *
 * **Headless.** No preview surface is created; a consumer that wants one attaches its own. **The consumer
 * holds the `CAMERA` permission** before [start] — this scanner reports a [CameraError.PermissionDenied]
 * on [results], it never requests permission (`scope.md` "permission boundary").
 *
 * **Verification status.** The contract this implements (the [MrzCameraScanner] interface and the
 * [scan][MrzFrameAnalyzer.scan] engine) is host-tested. The CameraX wiring is compiled on CI and was
 * verified end-to-end on a physical device (the live-device slice): the back camera opens, frames
 * stream, each [ImageProxy] is closed, and results flow. That slice established that CameraX surfaces a
 * failed open **asynchronously through camera state**, not as a bind-time exception — so this scanner
 * observes [CameraState] and routes a state error through [scan]'s catch path (see [cameraErrorFor]).
 * Permission denial was confirmed to surface a [CameraError.PermissionDenied]; the [CameraError.CameraInUse]
 * code-mapping is in place but the live in-use scenario (another client holding the camera) is not yet
 * device-exercised.
 *
 * @param appContext an application [Context] (held for the camera-provider lookup; pass
 *   `context.applicationContext` to avoid leaking an Activity).
 * @param lifecycleOwner the lifecycle the camera session is bound to; CameraX starts/stops the camera as
 *   this lifecycle moves, in addition to [start]/[stop].
 * @param recognizer the OCR seam. Defaults to the bundled ML Kit recognizer, which this scanner then owns
 *   and releases on [close]; a consumer-supplied recognizer that is [AutoCloseable] is likewise released
 *   on [close], since the scanner owns the OCR resource for its session.
 * @param mode strict (default) or lenient candidate extraction, forwarded to the analyse-frame core.
 * @param telemetry where per-frame [CameraFrameEvent]s go (default [NoOpTelemetrySink]).
 * @param cameraSelector which lens to use; defaults to the back camera (documents face the rear lens).
 */
public class CameraXMrzScanner(
    private val appContext: Context,
    private val lifecycleOwner: LifecycleOwner,
    recognizer: MrzTextRecognizer<ImageProxy> = MlKitMrzTextRecognizer(),
    mode: ParsingMode = ParsingMode.STRICT,
    telemetry: TelemetrySink = NoOpTelemetrySink,
    private val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
) : MrzCameraScanner,
    AutoCloseable {
    private val analyzer = MrzFrameAnalyzer(recognizer, mode, telemetry)
    private val ownedRecognizer = recognizer as? AutoCloseable

    // CameraX binds on the main thread; the analysis callbacks run on a dedicated single-thread executor.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    // Hot result stream, alive across start/stop. extraBufferCapacity + DROP_OLDEST means a slow collector
    // never suspends the analysis pipeline — it just misses the oldest pending result (the next frame is
    // milliseconds away), matching the "drop stale frames" intent of KEEP_ONLY_LATEST.
    private val mutableResults =
        MutableSharedFlow<MrzScanResult>(
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val results: Flow<MrzScanResult> = mutableResults.asSharedFlow()

    private var sessionJob: Job? = null

    override fun start() {
        if (sessionJob != null) return
        sessionJob =
            scope.launch {
                analyzer
                    .scan(
                        frames = cameraFrames(),
                        releaseFrame = ImageProxy::close,
                        captureErrorFor = ::cameraErrorFor,
                    ).collect(mutableResults::emit)
            }
    }

    override fun stop() {
        sessionJob?.cancel()
        sessionJob = null
    }

    /** Stops the session, releases the analysis executor, and closes the OCR recognizer it owns. */
    override fun close() {
        stop()
        scope.cancel()
        analysisExecutor.shutdown()
        ownedRecognizer?.close()
    }

    // CameraX's ImageAnalysis.Analyzer callback (an executor callback, not a coroutine) bridged to a Flow.
    // bindToLifecycle/unbindAll run on the collecting coroutine, which is on the main dispatcher. Binding
    // failures close the flow with the cause, which scan() maps to a CameraError via cameraErrorFor.
    private fun cameraFrames(): Flow<ImageProxy> =
        callbackFlow {
            val provider =
                try {
                    ProcessCameraProvider.getInstance(appContext).await()
                } catch (failure: Exception) {
                    close(failure)
                    return@callbackFlow
                }

            val analysis =
                ImageAnalysis
                    .Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
            analysis.setAnalyzer(analysisExecutor) { proxy ->
                // Hand the frame to the collector; if it is not ready, drop and close this frame here (a
                // dropped frame never reaches scan()'s releaseFrame). The RENDEZVOUS buffer (below) keeps
                // "not ready" to "the collector is mid-analysis" — the frame we want to drop anyway.
                if (trySendBlocking(proxy).isFailure) proxy.close()
            }

            val camera =
                try {
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
                } catch (failure: Exception) {
                    analysis.clearAnalyzer()
                    close(failure)
                    return@callbackFlow
                }

            // CameraX surfaces a failed camera open (permission denied, camera in use, hardware fault)
            // asynchronously through camera STATE, not as a bind-time exception — so without this the
            // flow would never emit and never close, and the consumer would get silence instead of a
            // typed CaptureError. Observing the camera state and closing the flow with the error code
            // routes it through scan()'s catch -> cameraErrorFor, exactly like a bind-time failure.
            val stateObserver =
                Observer<CameraState> { state ->
                    val stateError = state.error
                    if (stateError != null) close(CameraStateException(stateError.code))
                }
            camera.cameraInfo.cameraState.observe(lifecycleOwner, stateObserver)

            awaitClose {
                camera.cameraInfo.cameraState.removeObserver(stateObserver)
                analysis.clearAnalyzer()
                // Unbind only THIS session's use case, not the whole provider — so a stop()-then-start()
                // restart (whose old teardown may run after the new bind, since cancellation is
                // cooperative) cannot unbind the new session, and any other use case the consumer bound
                // to the same lifecycle is left alone. The full rapid-restart lifecycle is verified on a
                // device in the live-device slice.
                provider.unbind(analysis)
            }
        }.buffer(Channel.RENDEZVOUS)

    // Maps a camera-open failure to a typed CameraError. A CameraStateException carries the androidx
    // CameraState error code observed asynchronously (the path CameraX actually uses — see
    // cameraFrames); a SecurityException would be a synchronous bind-time throw (kept for completeness,
    // though on the devices tested CameraX does not throw it — it collapses to a CameraState error).
    private fun cameraErrorFor(cause: Throwable): CameraError? =
        when (cause) {
            is CameraStateException -> cameraStateError(cause.code)
            is SecurityException -> CameraError.PermissionDenied(cause.message ?: "CAMERA permission not granted")
            else -> CameraError.CameraUnavailable(cause.message ?: cause.toString())
        }

    // Classifies an androidx CameraState error code. In-use codes map cleanly. Everything else is some
    // flavour of "could not open": critically, CameraX collapses a PERMISSION denial into a generic
    // ERROR_CAMERA_FATAL_ERROR with a null cause — its public state API does not distinguish "no
    // permission" from a hardware fault. So for a non-in-use failure we read the observable CAMERA
    // permission state: if it is not held, PermissionDenied is the consumer's actionable cause;
    // otherwise the cause is genuinely unknown to us, so CameraUnavailable. This only *reads* the
    // permission — it never requests it or gates on it (scope.md permission boundary; reader-not-oracle:
    // we report the observable fact, we do not infer a cause we cannot see).
    private fun cameraStateError(code: Int): CameraError =
        when (code) {
            CameraState.ERROR_CAMERA_IN_USE, CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                CameraError.CameraInUse("camera is in use by another client (camera state error $code)")
            }

            else -> {
                if (!hasCameraPermission()) {
                    CameraError.PermissionDenied("CAMERA permission not granted (camera state error $code)")
                } else {
                    CameraError.CameraUnavailable("camera could not be opened (camera state error $code)")
                }
            }
        }

    private fun hasCameraPermission(): Boolean =
        appContext.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

// Carries the androidx [CameraState] error code from an asynchronous camera-open failure so
// [CameraXMrzScanner.cameraErrorFor] can map it to a [CameraError] through scan()'s catch path.
private class CameraStateException(
    val code: Int,
) : Exception("camera state error: $code")

// Bridges a Guava ListenableFuture (CameraX's provider lookup) to a suspend call without the
// kotlinx-coroutines-guava artifact — the same suspendCancellableCoroutine pattern the ML Kit Task
// bridge uses. The listener runs on the thread that completes the future; resuming the continuation
// from there is safe and the coroutine resumes on its own dispatcher.
private suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addListener({
            try {
                continuation.resume(get())
            } catch (failure: ExecutionException) {
                continuation.resumeWithException(failure.cause ?: failure)
            } catch (failure: Exception) {
                continuation.resumeWithException(failure)
            }
        }, Runnable::run)
        continuation.invokeOnCancellation { cancel(false) }
    }
