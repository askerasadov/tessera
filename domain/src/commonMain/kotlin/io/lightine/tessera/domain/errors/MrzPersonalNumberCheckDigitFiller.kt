package io.lightine.tessera.domain.errors

public data class MrzPersonalNumberCheckDigitFiller(
    val rawPersonalNumber: String,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() =
            "TD3 personal number check digit is the filler character '<' but the personal number itself contains " +
                "non-filler content: \"$rawPersonalNumber\". This is a documented real-world deviation from ICAO " +
                "Doc 9303 Part 4 strict conformance — some issuing states leave the personal-number check digit as " +
                "filler even when the personal number is populated. Surfaced as a warning rather than a validation " +
                "failure so consumers can decide whether to treat the deviation as disqualifying."
}
