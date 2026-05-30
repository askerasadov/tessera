package io.lightine.tessera.mrz.camera

/**
 * A capture-layer failure, surfaced on [`MrzScanResult.CaptureError`][MrzScanResult.CaptureError]
 * as a sealed result — never thrown, never crashing or hanging
 * ([ADR-020](https://github.com/lightine-io/tessera/blob/main/docs/decisions/0020-camera-reading-architecture.md)).
 * This is the `Camera…` family the [error taxonomy](https://github.com/lightine-io/tessera/blob/main/docs/features/mrz-error-taxonomy.md)
 * names, kept distinct from the `mrz-core` parse/validation taxonomy: it describes a failure to
 * *obtain* a reading, not a judgement about a reading's content.
 *
 * Each member carries a stable English [code] a consumer can switch on and localize; the
 * human-readable detail is for diagnostics, not end users.
 *
 * The family grows as the camera layers land. This slice — the analyse-frame core — produces only
 * [OcrFailed]; capture-availability failures (camera unavailable, permission denied, camera in use)
 * arrive with the owns-the-camera-session layer, each with the test that produces it, per the
 * project's new-error-type rule.
 */
public sealed interface CameraError {
    /** A stable, switch-on-able English code (e.g. `"camera.ocr_failed"`). Consumers localize it. */
    public val code: String

    /**
     * The platform OCR engine failed to process a frame — for example it threw, or reported an
     * error instead of a result. The analyse-frame core surfaces this rather than letting the
     * exception propagate, so the consumer can decide whether to retry on the next frame.
     */
    public data class OcrFailed(
        /** Human-readable diagnostic detail (not for display). Carries no document data. */
        public val message: String,
    ) : CameraError {
        override val code: String get() = "camera.ocr_failed"
    }
}
