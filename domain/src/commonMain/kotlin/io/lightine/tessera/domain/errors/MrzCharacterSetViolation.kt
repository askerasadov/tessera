package io.lightine.tessera.domain.errors

/**
 * Parse error: the input contains a character outside the MRZ alphabet (`A`–`Z`, `0`–`9`,
 * filler `<`). The first offending character is reported with its zero-based [position] in
 * the concatenated MRZ string.
 *
 * Emitted by [`MrzParser`][io.lightine.tessera.mrz.parsing.MrzParser] before any per-field
 * decoding, so a successful parse is guaranteed to contain only MRZ-alphabet characters.
 */
public data class MrzCharacterSetViolation(
    val offendingCharacter: Char,
    val position: Int,
) : MrzParseError() {
    override val description: String
        get() = "Character '$offendingCharacter' at position $position is outside the MRZ alphabet (A-Z, 0-9, <)"
}
