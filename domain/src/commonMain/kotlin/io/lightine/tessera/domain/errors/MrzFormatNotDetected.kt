package io.lightine.tessera.domain.errors

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
