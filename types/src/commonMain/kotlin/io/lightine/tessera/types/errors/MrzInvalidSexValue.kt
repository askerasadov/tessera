package io.lightine.tessera.types.errors

/**
 * Validation failure: the sex field contains a character outside the SDK's accepted set
 * (`M`, `F`, `<`, `X`). Carries the [observed] character and its zero-based [position]
 * in the concatenated MRZ string.
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. The SDK
 * does not coerce the value into a [`Sex`][io.lightine.tessera.types.vocabulary.Sex]
 * enum when it falls outside the accepted set (Principle 1 — reader, not oracle);
 * consumers see both the raw character (via `CommonFields.rawSex`) and this failure.
 *
 * **Spec note:** the ICAO Doc 9303 2021 Eighth Edition lists the *canonical* MRZ sex
 * characters as `M`, `F`, `<` only (per Part 4 §4.2.2.2 and the corresponding sections
 * in Parts 5/6/7). The character `X` is reserved for the **VIZ** per the spec's
 * Note p/f clauses. Real-world practice has nonetheless adopted `X` in the MRZ for
 * non-binary or unspecified documents, so the SDK accepts `X` without producing this
 * failure — it surfaces [MrzSexCharacterX] (a warning) instead. Characters outside
 * `{M, F, <, X}` entirely (e.g., `Z`, `0`) produce this validation failure.
 */
public data class MrzInvalidSexValue(
    val observed: Char,
    val position: Int,
) : MrzValidationError() {
    override val description: String
        get() =
            "Sex field at position $position contains '$observed'; accepted values are " +
                "'M', 'F', '<', or 'X' (X surfaces as MrzSexCharacterX warning rather than this failure)"
}
