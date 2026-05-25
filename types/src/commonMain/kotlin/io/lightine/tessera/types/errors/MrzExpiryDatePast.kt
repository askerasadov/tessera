package io.lightine.tessera.types.errors

import kotlinx.datetime.LocalDate

/**
 * Warning: the document's [expiryDate] is on or before [referenceDate] (the document is
 * expired).
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. A warning
 * rather than a validation failure because the SDK does not commit to "this document is
 * unusable" — many consumers process expired documents intentionally (e.g., historical
 * lookup, audit). Consumers who treat expiration as disqualifying read this warning out
 * of `ResultMetadata.warnings`.
 */
public data class MrzExpiryDatePast(
    val expiryDate: LocalDate,
    val referenceDate: LocalDate,
) : MrzWarning() {
    override val description: String
        get() = "Document expiry date $expiryDate is before reference date $referenceDate"
}
