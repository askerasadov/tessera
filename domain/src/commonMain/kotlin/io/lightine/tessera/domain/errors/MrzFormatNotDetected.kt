package io.lightine.tessera.domain.errors

/**
 * Parse error: the auto-detecting [`MrzParser.parse`][io.lightine.tessera.mrz.parsing.MrzParser]
 * could not match the input's line count and per-line lengths to any supported format
 * (TD1, TD2, TD3, MRV-A, MRV-B). Carries the observed shape so consumers can diagnose
 * the mismatch.
 *
 * Only emitted by the auto-detecting entry point; the format-specific entry points (e.g.,
 * `parseTD3`) surface [MrzInvalidLength] instead because they already know which format
 * was attempted.
 */
public data class MrzFormatNotDetected(
    val observedLineCount: Int,
    val observedLineLengths: List<Int>,
) : MrzParseError() {
    override val description: String
        get() =
            "MRZ format could not be detected from input shape: " +
                "$observedLineCount line(s) with lengths $observedLineLengths. " +
                "Expected one of: 3 lines of 30 characters (TD1), " +
                "2 lines of 36 characters (TD2 or MRV-B), " +
                "or 2 lines of 44 characters (TD3 or MRV-A)."
}
