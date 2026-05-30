package io.lightine.tessera.mrz.camera

/**
 * The text an OCR engine recognized in a single camera frame: the lines it read, in reading order.
 *
 * This is the raw OCR output, exposed verbatim (Principle 5 — transparency). [MrzFrameAnalyzer]
 * derives a candidate MRZ from it but never hides it: the recognized text travels on the
 * [MrzScanResult] alongside any parsed value, so a consumer can always see exactly what the engine
 * read. It is OCR output, not an MRZ — lines may be in any case, contain non-MRZ characters, carry
 * stray whitespace, or be absent entirely.
 */
public data class RecognizedText(
    /** Every line the engine recognized, in reading order. Empty when the engine found no text. */
    public val lines: List<RecognizedLine>,
)

/** One line of recognized text, with the engine's per-line confidence when it reports one. */
public data class RecognizedLine(
    /** The recognized characters of this line, exactly as the engine reported them. */
    public val text: String,
    /**
     * The engine's confidence in this line in `[0, 1]`, or `null` when the engine reports no
     * per-line confidence. A quality signal only — never used to gate a result.
     */
    public val confidence: Float?,
)
