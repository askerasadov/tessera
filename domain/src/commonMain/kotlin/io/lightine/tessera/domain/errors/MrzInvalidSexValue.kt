package io.lightine.tessera.domain.errors

/**
 * Validation failure: the sex field contains a character outside the ICAO Doc 9303
 * allowed set (`M`, `F`, `<`, `X`). Carries the [observed] character and its zero-based
 * [position] in the concatenated MRZ string.
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. The SDK
 * does not coerce the value into a [`Sex`][io.lightine.tessera.domain.vocabulary.Sex]
 * enum when it falls outside the allowed set (Principle 1 — reader, not oracle); consumers
 * see both the raw character (via `CommonFields.rawSex`) and this failure.
 */
public data class MrzInvalidSexValue(
    val observed: Char,
    val position: Int,
) : MrzValidationError() {
    override val description: String
        get() = "Sex field at position $position contains '$observed'; allowed values are 'M', 'F', '<', or 'X'"
}
