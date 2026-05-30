package io.lightine.tessera.mrz.camera

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageRelease
import platform.Vision.VNImageRequestHandler
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Simulator test for [VisionMrzTextRecognizer] — runs Apple Vision end-to-end on the
 * `iosSimulatorArm64` target (the Simulator has no camera, but Vision runs on a supplied image), via
 * the internal still-image seam the recognizer exposes for exactly this. The MRZ-specific OCR accuracy
 * on a real document is a device/printed-target concern (the same OCR-brittleness caveat Android
 * recorded), so this asserts the *pipeline* — handler → request config → `performRequests` → result
 * reading → ordering — rather than a positive MRZ decode.
 */
@OptIn(ExperimentalForeignApi::class)
class VisionMrzTextRecognizerTest {
    @Test
    fun runs_the_vision_pipeline_and_returns_no_lines_for_a_blank_image() {
        // A 64×64 blank RGBA image: a valid CGImage Vision can process, containing no text.
        // Core Graphics objects are Core Foundation types — Kotlin/Native's ARC bridge does not release
        // them, so they are released explicitly (the same discipline the owns-session capture code will need).
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val context =
            CGBitmapContextCreate(
                data = null,
                width = 64u,
                height = 64u,
                bitsPerComponent = 8u,
                bytesPerRow = 0u,
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
            )
        val image = CGBitmapContextCreateImage(context)
        try {
            val handler = VNImageRequestHandler(image, emptyMap<Any?, Any?>())

            val result = VisionMrzTextRecognizer().recognize(handler)

            // The pipeline ran end-to-end without throwing, and a blank image yields no recognized lines.
            assertTrue(result.lines.isEmpty(), "a blank image should produce no recognized lines")
        } finally {
            CGImageRelease(image)
            CGContextRelease(context)
            CGColorSpaceRelease(colorSpace)
        }
    }
}
