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
 * The family grows as the camera layers land. The analyse-frame core produces [OcrFailed]; the
 * owns-the-camera-session layer ([MrzCameraScanner]) adds the capture-availability failures
 * [CameraUnavailable], [PermissionDenied], and [CameraInUse] — each surfaced when the camera cannot
 * be started, never thrown. Their host-reproducible trigger is the scanner surfacing them as a sealed
 * result; the platform mapping from a real CameraX failure to the right member is verified on a device
 * (the live-device slice).
 */
public sealed interface CameraError {
    /** A stable, switch-on-able English code (e.g. `"camera.ocr_failed"`). Consumers localize it. */
    public val code: String

    /** Human-readable diagnostic detail (not for display, never document data). */
    public val message: String

    /**
     * The platform OCR engine failed to process a frame — for example it threw, or reported an
     * error instead of a result. The analyse-frame core surfaces this rather than letting the
     * exception propagate, so the consumer can decide whether to retry on the next frame.
     */
    public data class OcrFailed(
        override val message: String,
    ) : CameraError {
        override val code: String get() = "camera.ocr_failed"
    }

    /**
     * The camera could not be opened — no usable camera on the device, or the platform reported the
     * hardware as unavailable. Distinct from [PermissionDenied] (a usable camera the app may not yet
     * access) and [CameraInUse] (a usable camera another client holds).
     */
    public data class CameraUnavailable(
        override val message: String,
    ) : CameraError {
        override val code: String get() = "camera.unavailable"
    }

    /**
     * The camera permission is not granted, so the session cannot start. **Requesting the permission is
     * the consumer's responsibility** (`scope.md` "permission boundary"); the scanner only reports this
     * so the consumer can prompt and retry. The SDK never requests a permission on the consumer's behalf.
     */
    public data class PermissionDenied(
        override val message: String,
    ) : CameraError {
        override val code: String get() = "camera.permission_denied"
    }

    /**
     * The camera is held by another client (another app, or another part of this app) and cannot be
     * opened now. The consumer may retry once the other holder releases it.
     */
    public data class CameraInUse(
        override val message: String,
    ) : CameraError {
        override val code: String get() = "camera.in_use"
    }
}
