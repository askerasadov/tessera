package io.lightine.tessera.domain.errors

/**
 * Warning: the name field fills the available width with no trailing filler `<` character.
 * Per ICAO Doc 9303 convention a complete name always leaves at least one trailing filler,
 * so a field that fills exactly to the boundary is indistinguishable from a truncated one
 * and is treated as truncated.
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. Consumers
 * who need the truncated form can read it from `CommonFields.rawNameField`; the SDK does
 * not attempt to reconstruct the original name (Principle 1 — reader, not oracle).
 */
public data class MrzNameTruncated(
    val rawNameField: String,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() = "Name field at position $position fills the available width with no trailing filler, indicating ICAO Doc 9303 truncation"
}
