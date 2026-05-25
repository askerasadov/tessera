package io.lightine.tessera.mrz.model

import io.lightine.tessera.types.vocabulary.MrzFormat

/**
 * A parsed TD3 (passport) MRZ document per ICAO Doc 9303 Part 4. Two lines of 44
 * characters. Adds [personalNumber] (positions 28–42 on line 2) and
 * [personalNumberCheckDigit] (position 42 on line 2) beyond the [CommonFields] shared
 * with other formats.
 *
 * Note that some issuing states leave [personalNumberCheckDigit] as the filler character
 * `<` even when [personalNumber] is populated; the SDK surfaces this as
 * [`MrzPersonalNumberCheckDigitFiller`][io.lightine.tessera.types.errors.MrzPersonalNumberCheckDigitFiller]
 * rather than a check digit failure (real-world deviation, not strict ICAO conformance).
 */
public data class TD3(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val personalNumber: String,
    val personalNumberCheckDigit: Char,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.TD3
}
