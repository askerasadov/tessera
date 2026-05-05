package io.lightine.tessera.mrz.validation

import io.lightine.tessera.domain.MrzCheckDigitMismatch
import io.lightine.tessera.domain.MrzDateNotInCalendar
import io.lightine.tessera.domain.MrzExpiryDateImplausiblyFar
import io.lightine.tessera.domain.MrzExpiryDatePast
import io.lightine.tessera.domain.MrzField
import io.lightine.tessera.domain.MrzInvalidSexValue
import io.lightine.tessera.domain.MrzValidationError
import io.lightine.tessera.domain.MrzWarning
import io.lightine.tessera.mrz.MrzDate
import io.lightine.tessera.mrz.MrzDocument
import io.lightine.tessera.mrz.TD1
import io.lightine.tessera.mrz.TD3
import io.lightine.tessera.mrz.checkdigit.computeCheckDigit
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

public object MrzValidator {
    private val VALID_SEX_CHARACTERS = setOf('M', 'F', '<', 'X')
    private const val EXPIRY_IMPLAUSIBLY_FAR_THRESHOLD_YEARS = 10

    public fun validate(
        document: MrzDocument,
        referenceTime: Instant = Clock.System.now(),
    ): ValidationResult =
        when (document) {
            is TD3 -> validateTD3(document, referenceTime)
            is TD1 -> ValidationResult(validationFailures = emptyList(), warnings = emptyList())
        }

    private fun validateTD3(
        document: TD3,
        referenceTime: Instant,
    ): ValidationResult {
        val line2 = document.rawLines[1]
        val line2GlobalOffset = TD3_LINE_LENGTH

        val failures = mutableListOf<MrzValidationError>()

        addCheckDigitFailureIfMismatch(
            into = failures,
            input = line2.substring(0, 9),
            observed = document.commonFields.checkDigits.documentNumber,
            field = MrzField.DOCUMENT_NUMBER,
            position = line2GlobalOffset + 9,
        )
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = line2.substring(13, 19),
            observed = document.commonFields.checkDigits.dateOfBirth,
            field = MrzField.DATE_OF_BIRTH,
            position = line2GlobalOffset + 19,
        )
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = line2.substring(21, 27),
            observed = document.commonFields.checkDigits.dateOfExpiry,
            field = MrzField.DATE_OF_EXPIRY,
            position = line2GlobalOffset + 27,
        )
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = line2.substring(28, 42),
            observed = document.personalNumberCheckDigit,
            field = MrzField.OPTIONAL_DATA,
            position = line2GlobalOffset + 42,
        )

        val compositeInput = line2.substring(0, 10) + line2.substring(13, 20) + line2.substring(21, 43)
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = compositeInput,
            observed = document.commonFields.checkDigits.composite,
            field = MrzField.COMPOSITE,
            position = line2GlobalOffset + 43,
        )

        val rawSex = document.commonFields.rawSex
        if (rawSex !in VALID_SEX_CHARACTERS) {
            failures += MrzInvalidSexValue(observed = rawSex, position = line2GlobalOffset + 20)
        }

        addDateNotInCalendarFailureIfApplicable(
            into = failures,
            date = document.commonFields.dateOfBirth,
            field = MrzField.DATE_OF_BIRTH,
            position = line2GlobalOffset + 13,
        )
        addDateNotInCalendarFailureIfApplicable(
            into = failures,
            date = document.commonFields.dateOfExpiry,
            field = MrzField.DATE_OF_EXPIRY,
            position = line2GlobalOffset + 21,
        )

        val warnings = mutableListOf<MrzWarning>()
        addExpiryWarningsIfApplicable(
            into = warnings,
            expiryComputedDate = document.commonFields.dateOfExpiry.computedDate,
            referenceTime = referenceTime,
        )

        return ValidationResult(validationFailures = failures.toList(), warnings = warnings.toList())
    }

    private fun addDateNotInCalendarFailureIfApplicable(
        into: MutableList<MrzValidationError>,
        date: MrzDate,
        field: MrzField,
        position: Int,
    ) {
        if (date.componentsFormCalendarDate == false) {
            into +=
                MrzDateNotInCalendar(
                    field = field,
                    rawYear = date.rawYear,
                    rawMonth = date.rawMonth,
                    rawDay = date.rawDay,
                    position = position,
                )
        }
    }

    private fun addCheckDigitFailureIfMismatch(
        into: MutableList<MrzValidationError>,
        input: String,
        observed: Char,
        field: MrzField,
        position: Int,
    ) {
        val expected = computeCheckDigit(input)
        if (expected != observed) {
            into += MrzCheckDigitMismatch(field = field, expected = expected, observed = observed, position = position)
        }
    }

    private fun addExpiryWarningsIfApplicable(
        into: MutableList<MrzWarning>,
        expiryComputedDate: LocalDate?,
        referenceTime: Instant,
    ) {
        if (expiryComputedDate == null) return
        val referenceDate = referenceTime.toLocalDateTime(TimeZone.UTC).date
        if (expiryComputedDate < referenceDate) {
            into += MrzExpiryDatePast(expiryDate = expiryComputedDate, referenceDate = referenceDate)
            return
        }
        val implausiblyFarThreshold = referenceDate.plus(EXPIRY_IMPLAUSIBLY_FAR_THRESHOLD_YEARS, DateTimeUnit.YEAR)
        if (expiryComputedDate > implausiblyFarThreshold) {
            into +=
                MrzExpiryDateImplausiblyFar(
                    expiryDate = expiryComputedDate,
                    referenceDate = referenceDate,
                    thresholdYears = EXPIRY_IMPLAUSIBLY_FAR_THRESHOLD_YEARS,
                )
        }
    }

    private const val TD3_LINE_LENGTH = 44
}
