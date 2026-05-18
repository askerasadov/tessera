package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzFormat

/**
 * Generation error: a name field (primary or secondary identifier) contains numeric
 * characters, which ICAO Doc 9303 Part 3 §4.6 forbids in MRZ name fields.
 *
 * Spec text from §4.6: *"Numeric characters shall not be used in the name fields of
 * the MRZ."*
 *
 * Emitted by the primitive-input methods of
 * [`MrzGenerator`][io.lightine.tessera.mrz.generation.MrzGenerator] (`generateTD3`,
 * `generateTD2`, etc.) when [observedValue] (the consumer's `primaryIdentifier` or
 * `secondaryIdentifier` argument) contains digits. The SDK does not strip the digits
 * silently (Principle 1 — reader, not oracle); the consumer must decide how to handle
 * them (e.g., remove them, transliterate spelled-out forms, or reject the input).
 *
 * [observedValue] carries the original consumer-supplied string (before transliteration);
 * [numericCharacters] lists each digit encountered, in order of appearance.
 */
public data class MrzGenerationNumericInNameField(
    val format: MrzFormat,
    val observedValue: String,
    val numericCharacters: List<Char>,
) : MrzGenerationError() {
    override val description: String
        get() {
            val distinct = numericCharacters.distinct().joinToString(", ") { "'$it'" }
            return "Format $format name field contains numeric character(s) $distinct " +
                "(ICAO Doc 9303 Part 3 §4.6 forbids numeric characters in MRZ name fields). " +
                "Observed value: \"$observedValue\""
        }
}
