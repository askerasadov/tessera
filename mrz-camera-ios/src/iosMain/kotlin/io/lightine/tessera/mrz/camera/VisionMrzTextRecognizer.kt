package io.lightine.tessera.mrz.camera

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.Foundation.NSError
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * The iOS [MrzTextRecognizer], backed by Apple Vision (`VNRecognizeTextRequest`). It reads each
 * AVFoundation [CMSampleBufferRef][platform.CoreMedia.CMSampleBufferRef], extracts the pixel buffer,
 * runs Vision on-device, and returns the recognized lines for [MrzFrameAnalyzer] to turn into an MRZ
 * candidate. It makes no trust decision about what it reads — OCR only. The iOS analogue of the
 * Android `MlKitMrzTextRecognizer`, binding the analyse-frame core's frame type to `F = CMSampleBufferRef`
 * (the type an `AVCaptureVideoDataOutput` delivers), exactly as Android binds `F = ImageProxy`.
 *
 * **Vision configuration.** [`recognitionLevel`][VNRecognizeTextRequest.recognitionLevel] is `.accurate`
 * and **[`usesLanguageCorrection`][VNRecognizeTextRequest.usesLanguageCorrection] is `false`**: the MRZ is
 * not natural language, so language correction would *change* characters — the reader-not-oracle stance
 * forbids that. Recognized observations are ordered top-to-bottom (Vision does not guarantee reading
 * order) so the analyzer sees the MRZ lines in document order.
 *
 * **Frame ownership.** This reads the sample buffer but never releases it; the caller that produced the
 * frame (the `AVCaptureVideoDataOutput` delegate, wired up by the owns-the-camera-session layer) owns its
 * lifetime, exactly as AVFoundation requires. The recognizer holds no reference to the frame after
 * [recognize] returns (memory hygiene).
 *
 * **Threading.** Vision's `performRequests` is synchronous and CPU-bound; [recognize] runs it on the
 * calling coroutine's thread. The owns-the-camera-session layer drives frames on a background dispatch
 * queue, off the main thread.
 *
 * **PII.** Recognized MRZ text is kept out of any thrown failure message — on a Vision failure there is
 * no recognized text to leak, and the failure surfaces as [`CameraError.OcrFailed`][CameraError.OcrFailed]
 * carrying only Vision's own error description.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class VisionMrzTextRecognizer : MrzTextRecognizer<CMSampleBufferRef> {
    override suspend fun recognize(frame: CMSampleBufferRef): RecognizedText {
        // No backing image buffer (a malformed or non-video sample): nothing to recognize.
        val pixelBuffer = CMSampleBufferGetImageBuffer(frame) ?: return RecognizedText(emptyList())
        return recognize(VNImageRequestHandler(pixelBuffer, emptyMap<Any?, Any?>()))
    }

    // The Vision request itself, over a prepared image handler. The public CMSampleBuffer path builds a
    // handler from the frame's pixel buffer; a test builds one from a still image (CGImage) so the
    // request config, the run, the result reading, and the top-to-bottom ordering are all exercised on
    // the simulator without a camera. Throwing on a Vision failure is the seam's contract: MrzFrameAnalyzer
    // catches it and surfaces CameraError.OcrFailed.
    internal fun recognize(handler: VNImageRequestHandler): RecognizedText {
        val request =
            VNRecognizeTextRequest().apply {
                setRecognitionLevel(VNRequestTextRecognitionLevelAccurate)
                setUsesLanguageCorrection(false)
            }

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val ok = handler.performRequests(listOf(request), errorPtr.ptr)
            if (!ok) {
                val message = errorPtr.value?.localizedDescription ?: "Vision text recognition failed"
                throw VisionRecognitionException(message)
            }
        }

        @Suppress("UNCHECKED_CAST")
        val observations = (request.results as? List<VNRecognizedTextObservation>).orEmpty()

        return RecognizedText(
            observations
                // Vision does not guarantee reading order; sort by vertical position. Its normalized
                // coordinate origin is bottom-left, so a larger boundingBox.origin.y is higher on the
                // page — descending y is top-to-bottom reading order.
                .sortedByDescending { it.boundingBox.useContents { origin.y } }
                .mapNotNull { observation ->
                    val candidate = observation.topCandidates(1u).firstOrNull() as? VNRecognizedText
                    candidate?.let { RecognizedLine(text = it.string, confidence = it.confidence) }
                },
        )
    }
}

// Carries Vision's own error description from a failed performRequests so MrzFrameAnalyzer can surface
// it as a CameraError.OcrFailed. Never carries recognized text (there is none on failure).
private class VisionRecognitionException(
    message: String,
) : Exception(message)
