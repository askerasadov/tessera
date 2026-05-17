package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField

/**
 * Validation failure: the check digit recorded on the MRZ for a given [field] does not
 * match the value the SDK computes from the field's content per ICAO Doc 9303. Carries
 * the [expected] (computed) and [observed] (recorded) digits and the [position] of the
 * recorded digit in the concatenated MRZ string.
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. A check
 * digit mismatch means the document is structurally well-formed but does not internally
 * agree; it may indicate transcription error, deliberate tampering, or a non-conforming
 * issuer.
 */
public data class MrzCheckDigitMismatch(
    val field: MrzField,
    val expected: Char,
    val observed: Char,
    val position: Int,
) : MrzValidationError() {
    override val description: String
        get() = "Check digit for ${this.field} at position $position: expected '$expected', observed '$observed'"
}
