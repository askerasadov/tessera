package io.lightine.tessera.mrz.camera

import kotlinx.coroutines.flow.Flow

/**
 * The owns-the-camera-session convenience: the SDK runs the platform camera internally and streams a
 * [MrzScanResult] per analysed frame, so the consumer never touches `bindToLifecycle` / `ImageAnalysis`
 * (Android) or `AVCaptureSession` (iOS). It is the second of the two camera-reading layers
 * ([ADR-020](https://github.com/lightine-io/tessera/blob/main/docs/decisions/0020-camera-reading-architecture.md)),
 * built on top of the analyse-frame core ([MrzFrameAnalyzer]) — the same OCR → extract → parse pipeline,
 * now driven by a live frame stream instead of a single hand-supplied frame.
 *
 * **Still headless.** The scanner draws nothing: a consumer that wants a live preview attaches its own
 * preview surface. **Permission requests and camera availability remain the consumer's responsibility**
 * (`scope.md` "permission boundary"); when a capture cannot proceed the scanner *reports* a typed
 * [CameraError] on [results] as a [`MrzScanResult.CaptureError`][MrzScanResult.CaptureError] — it never
 * throws from the stream, crashes, or hangs (ADR-020).
 *
 * **Contract shape (provisional).** This interface — and the [MrzFrameAnalyzer.scan] engine it is built
 * on — is the UI-agnostic, frame-source-agnostic contract that iOS mirrors. Per ADR-020 the shape stays
 * provisional until the `0.2.0` tag, validated against the AVFoundation implementation before it locks
 * under [ADR-007](https://github.com/lightine-io/tessera/blob/main/docs/decisions/0007-strict-backward-compat-from-0x.md).
 */
public interface MrzCameraScanner {
    /**
     * The stream of results, one per analysed frame, plus any capture-availability failure surfaced as a
     * terminal [`MrzScanResult.CaptureError`][MrzScanResult.CaptureError]. A hot stream: it emits only
     * while the scanner is running (between [start] and [stop]); collectors that join late see results
     * from that point on, not a replay. In a live stream the consumer reads each result and waits for the
     * next — a noisy frame is simply [`NoMrzFound`][MrzScanResult.NoMrzFound] and the next clean frame
     * arrives within milliseconds (strict + next-frame retry).
     */
    public val results: Flow<MrzScanResult>

    /**
     * Starts the camera session and begins emitting on [results]. Idempotent: calling [start] on an
     * already-running scanner does nothing. The consumer must hold the camera permission first
     * (the scanner reports, it does not request).
     *
     * **Threading.** The lifecycle methods ([start], [stop], and a platform `close`) are not
     * thread-safe; call them from a single thread — typically the UI thread / Swift main actor, the
     * idiomatic place for camera lifecycle. Concurrent calls from multiple threads are not supported and
     * may race (e.g. two `start`s both passing the idempotence check). Collecting [results], by contrast,
     * is safe from any coroutine.
     */
    public fun start()

    /**
     * Stops the camera session and ends emission on [results]. Idempotent. After [stop] the scanner may
     * be [start]ed again. Releasing the scanner entirely (closing its camera resources) is the platform
     * type's own lifecycle concern, documented there.
     */
    public fun stop()
}
