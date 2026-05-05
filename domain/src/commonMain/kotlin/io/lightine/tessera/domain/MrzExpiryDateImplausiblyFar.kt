package io.lightine.tessera.domain

import kotlinx.datetime.LocalDate

public data class MrzExpiryDateImplausiblyFar(
    val expiryDate: LocalDate,
    val referenceDate: LocalDate,
    val thresholdYears: Int,
) : MrzWarning() {
    override val description: String
        get() = "Document expiry date $expiryDate is more than $thresholdYears years after reference date $referenceDate"
}
