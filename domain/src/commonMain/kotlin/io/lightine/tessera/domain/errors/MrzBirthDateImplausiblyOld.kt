package io.lightine.tessera.domain.errors

import kotlinx.datetime.LocalDate

public data class MrzBirthDateImplausiblyOld(
    val rawYear: String,
    val rawMonth: String,
    val rawDay: String,
    val referenceDate: LocalDate,
    val thresholdYears: Int,
) : MrzWarning() {
    override val description: String
        get() =
            "Document birth date components (year='$rawYear', month='$rawMonth', day='$rawDay') " +
                "imply an age greater than $thresholdYears years at every candidate century " +
                "relative to reference date $referenceDate"
}
