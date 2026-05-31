package io.lightine.tessera.mrz.camera

import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Unit test for [AVCaptureMrzScanner]'s capture-failure → [CameraError] mapping (`cameraErrorFor`).
 *
 * The live failures that feed this mapping cannot be exercised here: the iOS Simulator has no camera (so
 * the permission / in-use / no-device paths never run), and a session *runtime error* cannot be triggered
 * deliberately even on a real device — it is a genuine media-services / hardware fault, not something an
 * app can summon (see `docs/open-questions.md`). So this asserts the mapping *contract* directly: every
 * failure the scanner detects is surfaced to the consumer as the correct typed [CameraError] carrying the
 * original message — the iOS half of the same "report the observable failure as a sealed result, never
 * throw, never decide for the caller" contract the Android scanner follows
 * ([ADR-020](https://github.com/lightine-io/tessera/blob/main/docs/decisions/0020-camera-reading-architecture.md);
 * reader-not-oracle). The exceptions and `cameraErrorFor` are `internal` purely to make this reachable.
 */
@OptIn(ExperimentalForeignApi::class)
class AVCaptureMrzScannerErrorMappingTest {
    private fun mapping(cause: Throwable): CameraError? {
        // Constructing the scanner touches no camera (capture setup is lazy, on start()); close() just
        // cancels its scope and the owned recognizer.
        val scanner = AVCaptureMrzScanner()
        try {
            return scanner.cameraErrorFor(cause)
        } finally {
            scanner.close()
        }
    }

    @Test
    fun permission_failure_is_surfaced_as_PermissionDenied() {
        val error = assertIs<CameraError.PermissionDenied>(mapping(CameraPermissionException("permission not granted")))
        assertEquals("permission not granted", error.message, "the original failure message is preserved")
    }

    @Test
    fun in_use_interruption_is_surfaced_as_CameraInUse() {
        val error = assertIs<CameraError.CameraInUse>(mapping(CameraInUseException("held by another client")))
        assertEquals("held by another client", error.message)
    }

    @Test
    fun runtime_error_is_surfaced_as_CameraUnavailable() {
        // The path that is otherwise un-triggerable: a session runtime error closes the channel with a
        // CameraUnavailableException, which must reach the consumer as CameraUnavailable.
        val error = assertIs<CameraError.CameraUnavailable>(mapping(CameraUnavailableException("session runtime error")))
        assertEquals("session runtime error", error.message)
    }

    @Test
    fun an_unclassified_failure_falls_back_to_CameraUnavailable() {
        // An open failure the scanner did not anticipate still reaches the consumer as a terminal
        // CameraUnavailable rather than being swallowed or thrown out of the results stream.
        val error = assertIs<CameraError.CameraUnavailable>(mapping(IllegalStateException("unexpected")))
        assertEquals("unexpected", error.message)
    }
}
