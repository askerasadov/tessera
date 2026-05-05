package io.lightine.tessera.mrz.validation

import io.lightine.tessera.domain.MrzCheckDigitMismatch
import io.lightine.tessera.domain.MrzField
import io.lightine.tessera.domain.MrzInvalidSexValue
import io.lightine.tessera.domain.MrzValidationError
import io.lightine.tessera.mrz.MrzDocument
import io.lightine.tessera.mrz.TD1
import io.lightine.tessera.mrz.TD3
import io.lightine.tessera.mrz.checkdigit.computeCheckDigit

public object MrzValidator {
    private val VALID_SEX_CHARACTERS = setOf('M', 'F', '<', 'X')

    public fun validate(document: MrzDocument): ValidationResult =
        when (document) {
            is TD3 -> validateTD3(document)
            is TD1 -> ValidationResult(validationFailures = emptyList(), warnings = emptyList())
        }

    private fun validateTD3(document: TD3): ValidationResult {
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

        return ValidationResult(validationFailures = failures.toList(), warnings = emptyList())
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

    private const val TD3_LINE_LENGTH = 44
}
