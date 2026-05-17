package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzFormat

/**
 * Parse error: a format-specific parse entry point (e.g.,
 * [`MrzParser.parseTD3`][io.lightine.tessera.mrz.parsing.MrzParser]) was given input
 * whose line count or per-line lengths do not match the format's required shape. Carries
 * both the expected and observed dimensions so consumers can diagnose the mismatch.
 *
 * The auto-detecting parse entry point surfaces [MrzFormatNotDetected] instead, since
 * it does not commit to a target format before checking shape.
 */
public data class MrzInvalidLength(
    val format: MrzFormat,
    val expectedLineCount: Int,
    val expectedLineLength: Int,
    val observedLineCount: Int,
    val observedLineLengths: List<Int>,
) : MrzParseError() {
    override val description: String
        get() =
            "Format $format expected $expectedLineCount lines of $expectedLineLength characters; " +
                "observed $observedLineCount lines with lengths $observedLineLengths"
}
