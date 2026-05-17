package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzFormat

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
