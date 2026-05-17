package io.lightine.tessera.domain.errors

/**
 * Warning: a TD3 document's personal number check digit is the filler character `<` even
 * though the personal number itself contains non-filler content. This is a documented
 * real-world deviation from strict ICAO Doc 9303 Part 4 conformance — some issuing states
 * leave the personal-number check digit as filler even when the personal number is
 * populated.
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. Surfaced
 * as a warning rather than a [MrzCheckDigitMismatch] validation failure so consumers can
 * decide whether to treat the deviation as disqualifying — strict consumers can flag the
 * warning; permissive consumers can ignore it.
 */
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
