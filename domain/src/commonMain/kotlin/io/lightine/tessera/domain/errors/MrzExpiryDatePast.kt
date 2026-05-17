package io.lightine.tessera.domain.errors

import kotlinx.datetime.LocalDate

public data class MrzExpiryDatePast(
    val expiryDate: LocalDate,
    val referenceDate: LocalDate,
) : MrzWarning() {
    override val description: String
        get() = "Document expiry date $expiryDate is before reference date $referenceDate"
}
