package io.lightine.tessera.mrz.validation

import io.lightine.tessera.domain.errors.MrzBirthDateImplausiblyOld
import io.lightine.tessera.domain.errors.MrzCheckDigitMismatch
import io.lightine.tessera.domain.errors.MrzDateNotInCalendar
import io.lightine.tessera.domain.errors.MrzExpiryDateImplausiblyFar
import io.lightine.tessera.domain.errors.MrzExpiryDatePast
import io.lightine.tessera.domain.errors.MrzInvalidSexValue
import io.lightine.tessera.domain.errors.MrzNameTruncated
import io.lightine.tessera.domain.errors.MrzUnknownCountryCode
import io.lightine.tessera.domain.errors.MrzUnknownDocumentTypeCode
import io.lightine.tessera.domain.errors.MrzValidationError
import io.lightine.tessera.domain.errors.MrzWarning
import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.mrz.checkdigit.computeCheckDigit
import io.lightine.tessera.mrz.formats.Td3FormatSpec
import io.lightine.tessera.mrz.formats.extractFrom
import io.lightine.tessera.mrz.model.MrzDate
import io.lightine.tessera.mrz.model.MrzDocument
import io.lightine.tessera.mrz.model.TD1
import io.lightine.tessera.mrz.model.TD3
import io.lightine.tessera.mrz.recognition.CountryCode
import io.lightine.tessera.mrz.recognition.DocumentType
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
        val rawLines = document.rawLines
        val failures = mutableListOf<MrzValidationError>()

        addCheckDigitFailureIfMismatch(
            into = failures,
            input = Td3FormatSpec.documentNumber.extractFrom(rawLines),
            observed = document.commonFields.checkDigits.documentNumber,
            field = MrzField.DOCUMENT_NUMBER,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.documentNumberCheckDigit),
        )
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = Td3FormatSpec.dateOfBirth.extractFrom(rawLines),
            observed = document.commonFields.checkDigits.dateOfBirth,
            field = MrzField.DATE_OF_BIRTH,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.dateOfBirthCheckDigit),
        )
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = Td3FormatSpec.dateOfExpiry.extractFrom(rawLines),
            observed = document.commonFields.checkDigits.dateOfExpiry,
            field = MrzField.DATE_OF_EXPIRY,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.dateOfExpiryCheckDigit),
        )
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = Td3FormatSpec.personalNumber.extractFrom(rawLines),
            observed = document.personalNumberCheckDigit,
            field = MrzField.OPTIONAL_DATA,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.personalNumberCheckDigit),
        )

        val compositeInput = Td3FormatSpec.compositeInputFields.joinToString("") { it.extractFrom(rawLines) }
        addCheckDigitFailureIfMismatch(
            into = failures,
            input = compositeInput,
            observed = document.commonFields.checkDigits.composite,
            field = MrzField.COMPOSITE,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.compositeCheckDigit),
        )

        val rawSex = document.commonFields.rawSex
        if (rawSex !in VALID_SEX_CHARACTERS) {
            failures += MrzInvalidSexValue(observed = rawSex, position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.sex))
        }

        addDateNotInCalendarFailureIfApplicable(
            into = failures,
            date = document.commonFields.dateOfBirth,
            field = MrzField.DATE_OF_BIRTH,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.dateOfBirth),
        )
        addDateNotInCalendarFailureIfApplicable(
            into = failures,
            date = document.commonFields.dateOfExpiry,
            field = MrzField.DATE_OF_EXPIRY,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.dateOfExpiry),
        )

        val warnings = mutableListOf<MrzWarning>()
        addExpiryWarningsIfApplicable(
            into = warnings,
            expiryComputedDate = document.commonFields.dateOfExpiry.computedDate,
            referenceTime = referenceTime,
        )
        addBirthAgeWarningIfApplicable(
            into = warnings,
            birthDate = document.commonFields.dateOfBirth,
            referenceTime = referenceTime,
        )
        addUnknownDocumentTypeCodeWarningIfApplicable(
            into = warnings,
            documentType = document.commonFields.documentType,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.documentType),
        )
        addUnknownCountryCodeWarningIfApplicable(
            into = warnings,
            countryCode = document.commonFields.issuingState,
            field = MrzField.ISSUING_STATE,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.issuingState),
        )
        addUnknownCountryCodeWarningIfApplicable(
            into = warnings,
            countryCode = document.commonFields.nationality,
            field = MrzField.NATIONALITY,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.nationality),
        )
        addNameTruncatedWarningIfApplicable(
            into = warnings,
            nameTruncated = document.commonFields.nameTruncated,
            rawNameField = document.commonFields.rawNameField,
            position = Td3FormatSpec.globalPositionOf(Td3FormatSpec.rawNameField),
        )

        return ValidationResult(validationFailures = failures.toList(), warnings = warnings.toList())
    }

    private fun addNameTruncatedWarningIfApplicable(
        into: MutableList<MrzWarning>,
        nameTruncated: Boolean,
        rawNameField: String,
        position: Int,
    ) {
        if (!nameTruncated) return
        into += MrzNameTruncated(rawNameField = rawNameField, position = position)
    }

    private fun addUnknownDocumentTypeCodeWarningIfApplicable(
        into: MutableList<MrzWarning>,
        documentType: DocumentType,
        position: Int,
    ) {
        if (documentType.isRecognized) return
        into += MrzUnknownDocumentTypeCode(rawCode = documentType.rawCode, position = position)
    }

    private fun addUnknownCountryCodeWarningIfApplicable(
        into: MutableList<MrzWarning>,
        countryCode: CountryCode,
        field: MrzField,
        position: Int,
    ) {
        if (countryCode.isRecognized) return
        into += MrzUnknownCountryCode(field = field, rawCode = countryCode.rawCode, position = position)
    }

    private fun addBirthAgeWarningIfApplicable(
        into: MutableList<MrzWarning>,
        birthDate: MrzDate,
        referenceTime: Instant,
    ) {
        if (birthDate.componentsExceedBirthAgeLimit != true) return
        val referenceDate = referenceTime.toLocalDateTime(TimeZone.UTC).date
        into +=
            MrzBirthDateImplausiblyOld(
                rawYear = birthDate.rawYear,
                rawMonth = birthDate.rawMonth,
                rawDay = birthDate.rawDay,
                referenceDate = referenceDate,
                thresholdYears = MrzDate.MAX_PLAUSIBLE_AGE_YEARS,
            )
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
}
