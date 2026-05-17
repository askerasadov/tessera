package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField

/**
 * Warning: the document's [rawCode] (in either the issuing-state or nationality position,
 * disambiguated by [field]) is not present in
 * [`CountryCodeTable`][io.lightine.tessera.mrz.recognition.CountryCodeTable].
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. The SDK's
 * country code table is deliberately a starter set — see
 * [`docs/features/lookup-tables.md`](https://github.com/askerasadov/Tessera/blob/main/docs/features/lookup-tables.md);
 * tracked in `docs/open-questions.md`. A warning rather than a validation failure per
 * [ADR-013](https://github.com/askerasadov/Tessera/blob/main/docs/decisions/0013-recognition-failures-are-warnings.md):
 * "this code isn't in our table" is not the same as "this code is invalid."
 *
 * Strict consumers who want to treat unrecognized codes as disqualifying check
 * `warnings.isEmpty()` together with `validationFailures.isEmpty()`.
 */
public data class MrzUnknownCountryCode(
    val field: MrzField,
    val rawCode: String,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() = "Country code '$rawCode' for ${this.field} at position $position is not in the SDK's recognized lookup tables"
}
