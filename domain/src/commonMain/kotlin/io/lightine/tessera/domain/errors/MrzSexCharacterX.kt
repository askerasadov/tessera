package io.lightine.tessera.domain.errors

/**
 * Warning: the sex field contains the character `X`, which the ICAO Doc 9303 2021
 * Eighth Edition does not list as a valid MRZ sex character.
 *
 * Per Part 4 §4.2.2.2, Part 5 §4.2.2.2, Part 6 §4.2.2.2, and Part 7 §4.2.2.2 /
 * §7.2.2.2, the MRZ sex character is canonically `F`, `M`, or `<` (filler for
 * unspecified) only. `X` is reserved for the **VIZ** ("Visual Inspection Zone")
 * per the Note p / f clauses, with the explicit instruction *"the filler character
 * (`<`) shall be used in this field in the MRZ and an X in this field in the VIZ"*.
 *
 * Real-world practice has nonetheless adopted `X` in the MRZ position for documents
 * issued by states using a non-binary or unspecified sex designation. The SDK accepts
 * `X` without rejection (Principle 1 — reader, not oracle: do not refuse documents the
 * real world actually issues) but surfaces this warning so consumers can identify the
 * spec deviation and decide how to handle it. Surfaced as a warning rather than as
 * [MrzInvalidSexValue] (which is reserved for characters outside `{M, F, <, X}` entirely).
 *
 * Strict consumers who want to treat the deviation as disqualifying check
 * `warnings.isEmpty()` together with `validationFailures.isEmpty()`. Permissive
 * consumers (the typical case) can ignore the warning.
 *
 * [observed] is always `'X'`; the field exists for symmetry with [MrzInvalidSexValue].
 */
public data class MrzSexCharacterX(
    val observed: Char,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() =
            "Sex field at position $position contains 'X' (non-binary / unspecified). " +
                "ICAO Doc 9303 2021 Eighth Edition lists the canonical MRZ sex characters as " +
                "'M', 'F', '<' only — 'X' is for the VIZ per the spec's Notes p/f; observed " +
                "real-world practice uses 'X' in the MRZ for some issuing states. Surfaced as " +
                "a warning rather than a validation failure so consumers can decide whether " +
                "to treat the deviation as disqualifying."
}
