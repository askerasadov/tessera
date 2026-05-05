package io.lightine.tessera.mrz.validation

import io.lightine.tessera.domain.MrzCheckDigitMismatch
import io.lightine.tessera.domain.MrzField
import io.lightine.tessera.domain.MrzFormat
import io.lightine.tessera.domain.MrzInvalidSexValue
import io.lightine.tessera.domain.Sex
import io.lightine.tessera.mrz.CommonFields
import io.lightine.tessera.mrz.DocumentType
import io.lightine.tessera.mrz.MrzCheckDigits
import io.lightine.tessera.mrz.MrzDate
import io.lightine.tessera.mrz.TD1
import io.lightine.tessera.mrz.TD3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MrzValidatorTest {
    private val specimenLine1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
    private val specimenLine2 = "L898902C<3UTO6908061F9406236ZE184226B<<<<<14"

    private fun specimenCommonFields(
        rawSex: Char = 'F',
        sex: Sex = Sex.FEMALE,
        checkDigits: MrzCheckDigits =
            MrzCheckDigits(
                documentNumber = '3',
                dateOfBirth = '1',
                dateOfExpiry = '6',
                optionalData = '1',
                composite = '4',
            ),
    ): CommonFields =
        CommonFields(
            documentType = DocumentType("P"),
            issuingState = "UTO",
            primaryIdentifier = "",
            secondaryIdentifier = "",
            nameTruncated = false,
            rawNameField = "ERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            documentNumber = "L898902C<",
            nationality = "UTO",
            dateOfBirth = MrzDate(rawYear = "69", rawMonth = "08", rawDay = "06"),
            sex = sex,
            rawSex = rawSex,
            dateOfExpiry = MrzDate(rawYear = "94", rawMonth = "06", rawDay = "23"),
            checkDigits = checkDigits,
        )

    private fun specimenTd3(
        line2: String = specimenLine2,
        commonFields: CommonFields = specimenCommonFields(),
        personalNumberCheckDigit: Char = '1',
    ): TD3 =
        TD3(
            rawLines = listOf(specimenLine1, line2),
            commonFields = commonFields,
            personalNumber = "ZE184226B<<<<<",
            personalNumberCheckDigit = personalNumberCheckDigit,
        )

    // --- Happy path ---

    @Test
    fun specimen_passes_all_check_digits_and_sex_validation() {
        val result = MrzValidator.validate(specimenTd3())
        assertTrue(result.validationFailures.isEmpty(), "Expected no failures; got ${result.validationFailures}")
        assertTrue(result.warnings.isEmpty(), "Expected no warnings; got ${result.warnings}")
    }

    // --- Per-field check digit failures ---

    @Test
    fun reports_document_number_check_digit_mismatch_with_expected_observed_and_position() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(
                        checkDigits =
                            MrzCheckDigits(
                                documentNumber = '7',
                                dateOfBirth = '1',
                                dateOfExpiry = '6',
                                optionalData = '1',
                                composite = '4',
                            ),
                    ),
            )
        val result = MrzValidator.validate(td3)
        val mismatch =
            assertIs<MrzCheckDigitMismatch>(
                result.validationFailures.first { it is MrzCheckDigitMismatch && it.field == MrzField.DOCUMENT_NUMBER },
            )
        assertEquals('3', mismatch.expected)
        assertEquals('7', mismatch.observed)
        assertEquals(53, mismatch.position)
    }

    @Test
    fun reports_date_of_birth_check_digit_mismatch() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(
                        checkDigits =
                            MrzCheckDigits(
                                documentNumber = '3',
                                dateOfBirth = '0',
                                dateOfExpiry = '6',
                                optionalData = '1',
                                composite = '4',
                            ),
                    ),
            )
        val result = MrzValidator.validate(td3)
        val mismatch =
            assertIs<MrzCheckDigitMismatch>(
                result.validationFailures.first { it is MrzCheckDigitMismatch && it.field == MrzField.DATE_OF_BIRTH },
            )
        assertEquals('1', mismatch.expected)
        assertEquals('0', mismatch.observed)
        assertEquals(63, mismatch.position)
    }

    @Test
    fun reports_date_of_expiry_check_digit_mismatch() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(
                        checkDigits =
                            MrzCheckDigits(
                                documentNumber = '3',
                                dateOfBirth = '1',
                                dateOfExpiry = '5',
                                optionalData = '1',
                                composite = '4',
                            ),
                    ),
            )
        val result = MrzValidator.validate(td3)
        val mismatch =
            assertIs<MrzCheckDigitMismatch>(
                result.validationFailures.first { it is MrzCheckDigitMismatch && it.field == MrzField.DATE_OF_EXPIRY },
            )
        assertEquals('6', mismatch.expected)
        assertEquals('5', mismatch.observed)
        assertEquals(71, mismatch.position)
    }

    @Test
    fun reports_optional_data_check_digit_mismatch() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(
                        checkDigits =
                            MrzCheckDigits(
                                documentNumber = '3',
                                dateOfBirth = '1',
                                dateOfExpiry = '6',
                                optionalData = '0',
                                composite = '4',
                            ),
                    ),
                personalNumberCheckDigit = '0',
            )
        val result = MrzValidator.validate(td3)
        val mismatch =
            assertIs<MrzCheckDigitMismatch>(
                result.validationFailures.first { it is MrzCheckDigitMismatch && it.field == MrzField.OPTIONAL_DATA },
            )
        assertEquals('1', mismatch.expected)
        assertEquals('0', mismatch.observed)
        assertEquals(86, mismatch.position)
    }

    @Test
    fun reports_composite_check_digit_mismatch() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(
                        checkDigits =
                            MrzCheckDigits(
                                documentNumber = '3',
                                dateOfBirth = '1',
                                dateOfExpiry = '6',
                                optionalData = '1',
                                composite = '0',
                            ),
                    ),
            )
        val result = MrzValidator.validate(td3)
        val mismatch =
            assertIs<MrzCheckDigitMismatch>(
                result.validationFailures.first { it is MrzCheckDigitMismatch && it.field == MrzField.COMPOSITE },
            )
        assertEquals('4', mismatch.expected)
        assertEquals('0', mismatch.observed)
        assertEquals(87, mismatch.position)
    }

    @Test
    fun reports_every_failed_check_digit_when_multiple_are_corrupt() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(
                        checkDigits =
                            MrzCheckDigits(
                                documentNumber = '0',
                                dateOfBirth = '0',
                                dateOfExpiry = '0',
                                optionalData = '0',
                                composite = '0',
                            ),
                    ),
                personalNumberCheckDigit = '0',
            )
        val result = MrzValidator.validate(td3)
        val mismatchedFields =
            result.validationFailures
                .filterIsInstance<MrzCheckDigitMismatch>()
                .map { it.field }
                .toSet()
        assertEquals(
            setOf(
                MrzField.DOCUMENT_NUMBER,
                MrzField.DATE_OF_BIRTH,
                MrzField.DATE_OF_EXPIRY,
                MrzField.OPTIONAL_DATA,
                MrzField.COMPOSITE,
            ),
            mismatchedFields,
        )
    }

    // --- Sex validation ---

    @Test
    fun accepts_male_female_filler_and_x_as_valid_sex_values() {
        for (validSex in listOf('M', 'F', '<', 'X')) {
            val td3 = specimenTd3(commonFields = specimenCommonFields(rawSex = validSex))
            val result = MrzValidator.validate(td3)
            assertTrue(
                result.validationFailures.none { it is MrzInvalidSexValue },
                "Sex value '$validSex' should not produce MrzInvalidSexValue; got ${result.validationFailures}",
            )
        }
    }

    @Test
    fun reports_invalid_sex_character_with_observed_and_position() {
        val td3 = specimenTd3(commonFields = specimenCommonFields(rawSex = 'Q', sex = Sex.UNSPECIFIED))
        val result = MrzValidator.validate(td3)
        val invalid =
            assertIs<MrzInvalidSexValue>(
                result.validationFailures.first { it is MrzInvalidSexValue },
            )
        assertEquals('Q', invalid.observed)
        assertEquals(64, invalid.position)
    }

    // --- TD1 (deferred path) ---

    @Test
    fun td1_validation_returns_empty_result_pending_td1_validator_implementation() {
        val td1 =
            TD1(
                rawLines =
                    listOf(
                        "I<UTOL898902C<0<<<<<<<<<<<<<<<",
                        "6908061F3008060UTO<<<<<<<<<<<0",
                        "ERIKSSON<<ANNA<MARIA<<<<<<<<<<",
                    ),
                commonFields = specimenCommonFields(),
                optionalData1 = "<<<<<<<<<<<<<<<",
                optionalData2 = "<<<<<<<<<<<",
            )
        val result = MrzValidator.validate(td1)
        assertEquals(MrzFormat.TD1, td1.format)
        assertTrue(result.validationFailures.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
}
