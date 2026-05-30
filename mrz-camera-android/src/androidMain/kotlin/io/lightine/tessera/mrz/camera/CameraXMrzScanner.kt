package io.lightine.tessera.mrz.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
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
 * [scan][MrzFrameAnalyzer.scan] engine) is host-tested; this CameraX wiring is compiled on CI and
 * verified end-to-end on a physical device in the live-device slice. The exact mapping from a CameraX
 * failure to a [CameraError] member — especially permission and camera-in-use, which CameraX can surface
 * asynchronously through camera state rather than as a bind-time exception — is refined against a real
 * device there.
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

            try {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
            } catch (failure: Exception) {
                analysis.clearAnalyzer()
                close(failure)
                return@callbackFlow
            }

            awaitClose {
                analysis.clearAnalyzer()
                // Unbind only THIS session's use case, not the whole provider — so a stop()-then-start()
                // restart (whose old teardown may run after the new bind, since cancellation is
                // cooperative) cannot unbind the new session, and any other use case the consumer bound
                // to the same lifecycle is left alone. The full rapid-restart lifecycle is verified on a
                // device in the live-device slice.
                provider.unbind(analysis)
            }
        }.buffer(Channel.RENDEZVOUS)

    // Maps a CameraX/camera-open failure to a typed CameraError. Conservative for this slice; the precise
    // mapping is shaken out on a device in the live-device slice (see the class KDoc).
    private fun cameraErrorFor(cause: Throwable): CameraError? =
        when (cause) {
            is SecurityException -> CameraError.PermissionDenied(cause.message ?: "camera permission not granted")
            else -> CameraError.CameraUnavailable(cause.message ?: cause.toString())
        }
}

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
