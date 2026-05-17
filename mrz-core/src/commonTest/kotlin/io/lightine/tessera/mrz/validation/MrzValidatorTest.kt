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
import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.Sex
import io.lightine.tessera.mrz.model.CommonFields
import io.lightine.tessera.mrz.model.MrzCheckDigits
import io.lightine.tessera.mrz.model.MrzDate
import io.lightine.tessera.mrz.model.MrzDateInferenceMethod
import io.lightine.tessera.mrz.model.TD3
import io.lightine.tessera.mrz.recognition.CountryCode
import io.lightine.tessera.mrz.recognition.DocumentType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
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
            issuingState = CountryCode("UTO"),
            primaryIdentifier = "",
            secondaryIdentifier = "",
            nameTruncated = false,
            rawNameField = "ERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            documentNumber = "L898902C<",
            nationality = CountryCode("UTO"),
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
        // Specimen uses fictional ICAO test code "UTO" (Utopia), intentionally not in the SDK starter set.
        // Two MrzUnknownCountryCode warnings are expected (issuingState + nationality); no others.
        val nonCountryWarnings = result.warnings.filterNot { it is MrzUnknownCountryCode }
        assertTrue(nonCountryWarnings.isEmpty(), "Expected no warnings beyond MrzUnknownCountryCode; got $nonCountryWarnings")
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

    // --- Expiry warnings ---

    private fun computedExpiry(date: LocalDate): MrzDate {
        val rawYear = (date.year % 100).toString().padStart(2, '0')
        val rawMonth = date.monthNumber.toString().padStart(2, '0')
        val rawDay = date.dayOfMonth.toString().padStart(2, '0')
        return MrzDate(
            rawYear = rawYear,
            rawMonth = rawMonth,
            rawDay = rawDay,
            computedYear = date.year,
            computedDate = date,
            inferenceMethod = MrzDateInferenceMethod.SLIDING_WINDOW_EXPIRY,
        )
    }

    private fun specimenWithExpiry(
        expiryDate: LocalDate,
        checkDigits: MrzCheckDigits = specimenCommonFields().checkDigits,
    ): TD3 {
        val baseFields = specimenCommonFields(checkDigits = checkDigits)
        return specimenTd3(commonFields = baseFields.copy(dateOfExpiry = computedExpiry(expiryDate)))
    }

    @Test
    fun expiry_in_past_emits_MrzExpiryDatePast_warning_with_dates() {
        val expiry = LocalDate(2020, 6, 23)
        val referenceTime = Instant.parse("2026-05-04T12:00:00Z")
        val td3 = specimenWithExpiry(expiry)

        val result = MrzValidator.validate(td3, referenceTime = referenceTime)

        val warning = assertIs<MrzExpiryDatePast>(result.warnings.first { it is MrzExpiryDatePast })
        assertEquals(expiry, warning.expiryDate)
        assertEquals(LocalDate(2026, 5, 4), warning.referenceDate)
        assertTrue(
            result.warnings.none { it is MrzExpiryDateImplausiblyFar },
            "Past-expiry case must not also emit implausibly-far warning",
        )
    }

    @Test
    fun expiry_equal_to_reference_date_emits_no_warning() {
        val sameDay = LocalDate(2026, 5, 4)
        val referenceTime = Instant.parse("2026-05-04T12:00:00Z")
        val td3 = specimenWithExpiry(sameDay)

        val result = MrzValidator.validate(td3, referenceTime = referenceTime)

        assertTrue(
            result.warnings.none { it is MrzExpiryDatePast || it is MrzExpiryDateImplausiblyFar },
            "Expiry equal to reference date is neither past nor implausibly far; got ${result.warnings}",
        )
    }

    @Test
    fun expiry_within_ten_years_after_reference_emits_no_warning() {
        val expiry = LocalDate(2031, 6, 23)
        val referenceTime = Instant.parse("2026-05-04T12:00:00Z")
        val td3 = specimenWithExpiry(expiry)

        val result = MrzValidator.validate(td3, referenceTime = referenceTime)

        assertTrue(
            result.warnings.none { it is MrzExpiryDatePast || it is MrzExpiryDateImplausiblyFar },
            "Expiry within ten years after reference must not warn; got ${result.warnings}",
        )
    }

    @Test
    fun expiry_exactly_ten_years_after_reference_emits_no_warning() {
        val expiry = LocalDate(2036, 5, 4)
        val referenceTime = Instant.parse("2026-05-04T12:00:00Z")
        val td3 = specimenWithExpiry(expiry)

        val result = MrzValidator.validate(td3, referenceTime = referenceTime)

        assertTrue(
            result.warnings.none { it is MrzExpiryDateImplausiblyFar },
            "Expiry exactly ten years after reference is on the boundary, not over it; got ${result.warnings}",
        )
    }

    @Test
    fun expiry_more_than_ten_years_after_reference_emits_implausibly_far_warning_with_threshold() {
        val expiry = LocalDate(2040, 1, 1)
        val referenceTime = Instant.parse("2026-05-04T12:00:00Z")
        val td3 = specimenWithExpiry(expiry)

        val result = MrzValidator.validate(td3, referenceTime = referenceTime)

        val warning =
            assertIs<MrzExpiryDateImplausiblyFar>(
                result.warnings.first { it is MrzExpiryDateImplausiblyFar },
            )
        assertEquals(expiry, warning.expiryDate)
        assertEquals(LocalDate(2026, 5, 4), warning.referenceDate)
        assertEquals(10, warning.thresholdYears)
        assertTrue(
            result.warnings.none { it is MrzExpiryDatePast },
            "Far-future case must not also emit past warning",
        )
    }

    @Test
    fun expiry_with_uncomputed_date_emits_no_warning_pending_date_in_calendar_check() {
        // Specimen as-shipped: rawYear/rawMonth/rawDay only, computedDate = null.
        val td3 = specimenTd3()
        val result = MrzValidator.validate(td3, referenceTime = Instant.parse("2050-01-01T00:00:00Z"))
        assertTrue(
            result.warnings.none { it is MrzExpiryDatePast || it is MrzExpiryDateImplausiblyFar },
            "When computedDate is null the validator has no date to compare; got ${result.warnings}",
        )
    }

    // --- Date in calendar ---

    @Test
    fun reports_MrzDateNotInCalendar_for_birth_date_when_componentsFormCalendarDate_is_false() {
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfBirth =
                            MrzDate(
                                rawYear = "90",
                                rawMonth = "02",
                                rawDay = "30",
                                componentsFormCalendarDate = false,
                            ),
                    ),
            )

        val result = MrzValidator.validate(td3)

        val failure =
            assertIs<MrzDateNotInCalendar>(
                result.validationFailures.first { it is MrzDateNotInCalendar },
            )
        assertEquals(MrzField.DATE_OF_BIRTH, failure.field)
        assertEquals("90", failure.rawYear)
        assertEquals("02", failure.rawMonth)
        assertEquals("30", failure.rawDay)
        assertEquals(57, failure.position)
    }

    @Test
    fun reports_MrzDateNotInCalendar_for_expiry_date_when_componentsFormCalendarDate_is_false() {
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfExpiry =
                            MrzDate(
                                rawYear = "30",
                                rawMonth = "13",
                                rawDay = "01",
                                componentsFormCalendarDate = false,
                            ),
                    ),
            )

        val result = MrzValidator.validate(td3)

        val failure =
            assertIs<MrzDateNotInCalendar>(
                result.validationFailures.first { it is MrzDateNotInCalendar },
            )
        assertEquals(MrzField.DATE_OF_EXPIRY, failure.field)
        assertEquals("30", failure.rawYear)
        assertEquals("13", failure.rawMonth)
        assertEquals("01", failure.rawDay)
        assertEquals(65, failure.position)
    }

    @Test
    fun reports_MrzDateNotInCalendar_for_both_dates_independently_when_both_components_invalid() {
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfBirth =
                            MrzDate(
                                rawYear = "90",
                                rawMonth = "02",
                                rawDay = "30",
                                componentsFormCalendarDate = false,
                            ),
                        dateOfExpiry =
                            MrzDate(
                                rawYear = "30",
                                rawMonth = "13",
                                rawDay = "01",
                                componentsFormCalendarDate = false,
                            ),
                    ),
            )

        val result = MrzValidator.validate(td3)

        val notInCalendarFields =
            result.validationFailures
                .filterIsInstance<MrzDateNotInCalendar>()
                .map { it.field }
                .toSet()
        assertEquals(setOf(MrzField.DATE_OF_BIRTH, MrzField.DATE_OF_EXPIRY), notInCalendarFields)
    }

    @Test
    fun does_not_report_MrzDateNotInCalendar_when_componentsFormCalendarDate_is_true_but_no_computed_date() {
        // Calendar-valid but outside the parser's inference window: the date IS in the calendar,
        // so MrzDateNotInCalendar must not fire even though computedDate is null.
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfExpiry =
                            MrzDate(
                                rawYear = "80",
                                rawMonth = "08",
                                rawDay = "15",
                                componentsFormCalendarDate = true,
                            ),
                    ),
            )

        val result = MrzValidator.validate(td3)

        assertTrue(
            result.validationFailures.none { it is MrzDateNotInCalendar },
            "Calendar-valid but out-of-window dates must not produce MrzDateNotInCalendar; got ${result.validationFailures}",
        )
    }

    @Test
    fun does_not_report_MrzDateNotInCalendar_when_componentsFormCalendarDate_is_null() {
        // Null = "components didn't parse as ints"; that is Layer-1 territory, not date-in-calendar.
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfBirth =
                            MrzDate(
                                rawYear = "ab",
                                rawMonth = "08",
                                rawDay = "06",
                                componentsFormCalendarDate = null,
                            ),
                    ),
            )

        val result = MrzValidator.validate(td3)

        assertTrue(
            result.validationFailures.none { it is MrzDateNotInCalendar },
            "Non-numeric components are not MrzDateNotInCalendar; got ${result.validationFailures}",
        )
    }

    @Test
    fun does_not_report_MrzDateNotInCalendar_for_specimen_with_default_signal() {
        // Specimen as-shipped uses the default componentsFormCalendarDate = null, since the test
        // fixture builds MrzDate via primary constructor without invoking parseBirth/parseExpiry.
        // No MrzDateNotInCalendar should fire — the fixture is "we don't know," not "no calendar."
        val result = MrzValidator.validate(specimenTd3())
        assertTrue(
            result.validationFailures.none { it is MrzDateNotInCalendar },
            "Default null signal must not trigger MrzDateNotInCalendar; got ${result.validationFailures}",
        )
    }

    // --- Birth-age warnings ---

    @Test
    fun birth_with_componentsExceedBirthAgeLimit_true_emits_MrzBirthDateImplausiblyOld_with_threshold_and_reference() {
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfBirth =
                            MrzDate(
                                rawYear = "00",
                                rawMonth = "06",
                                rawDay = "15",
                                componentsFormCalendarDate = true,
                                componentsExceedBirthAgeLimit = true,
                            ),
                    ),
            )

        val referenceTime = Instant.parse("2200-01-01T00:00:00Z")
        val result = MrzValidator.validate(td3, referenceTime = referenceTime)

        val warning =
            assertIs<MrzBirthDateImplausiblyOld>(
                result.warnings.first { it is MrzBirthDateImplausiblyOld },
            )
        assertEquals("00", warning.rawYear)
        assertEquals("06", warning.rawMonth)
        assertEquals("15", warning.rawDay)
        assertEquals(LocalDate(2200, 1, 1), warning.referenceDate)
        assertEquals(130, warning.thresholdYears)
    }

    @Test
    fun birth_with_componentsExceedBirthAgeLimit_false_does_not_emit_warning() {
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfBirth =
                            MrzDate(
                                rawYear = "80",
                                rawMonth = "06",
                                rawDay = "15",
                                computedYear = 1980,
                                computedDate = LocalDate(1980, 6, 15),
                                inferenceMethod = MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH,
                                componentsFormCalendarDate = true,
                                componentsExceedBirthAgeLimit = false,
                            ),
                    ),
            )
        val result = MrzValidator.validate(td3)
        assertTrue(
            result.warnings.none { it is MrzBirthDateImplausiblyOld },
            "Successful birth inference must not emit MrzBirthDateImplausiblyOld; got ${result.warnings}",
        )
    }

    @Test
    fun birth_with_componentsExceedBirthAgeLimit_null_does_not_emit_warning() {
        // Specimen as-shipped: dateOfBirth uses primary constructor with default null signal.
        val result = MrzValidator.validate(specimenTd3())
        assertTrue(
            result.warnings.none { it is MrzBirthDateImplausiblyOld },
            "Default null signal must not emit MrzBirthDateImplausiblyOld; got ${result.warnings}",
        )
    }

    @Test
    fun expiry_signal_does_not_influence_birth_age_warning() {
        // Even if (hypothetically) an expiry MrzDate carried componentsExceedBirthAgeLimit = true via direct
        // construction, the validator must consult only the birth date. Lock that the warning is birth-only.
        val baseFields = specimenCommonFields()
        val td3 =
            specimenTd3(
                commonFields =
                    baseFields.copy(
                        dateOfExpiry =
                            MrzDate(
                                rawYear = "30",
                                rawMonth = "06",
                                rawDay = "01",
                                componentsFormCalendarDate = true,
                                componentsExceedBirthAgeLimit = true,
                            ),
                    ),
            )
        val result = MrzValidator.validate(td3)
        assertTrue(
            result.warnings.none { it is MrzBirthDateImplausiblyOld },
            "Expiry-side signal must not trigger birth-age warning; got ${result.warnings}",
        )
    }

    @Test
    fun birth_parsed_at_far_future_reference_emits_MrzBirthDateImplausiblyOld_end_to_end() {
        val referenceTime = Instant.parse("2200-01-01T00:00:00Z")
        val parsedBirth =
            MrzDate.parseBirth(
                rawYear = "00",
                rawMonth = "06",
                rawDay = "15",
                referenceTime = referenceTime,
            )
        val baseFields = specimenCommonFields()
        val td3 = specimenTd3(commonFields = baseFields.copy(dateOfBirth = parsedBirth))

        val result = MrzValidator.validate(td3, referenceTime = referenceTime)

        val warning =
            assertIs<MrzBirthDateImplausiblyOld>(
                result.warnings.first { it is MrzBirthDateImplausiblyOld },
            )
        assertEquals(LocalDate(2200, 1, 1), warning.referenceDate)
        assertEquals(130, warning.thresholdYears)
    }

    // --- Unknown document type code warning ---

    @Test
    fun unrecognized_document_type_code_emits_MrzUnknownDocumentTypeCode_with_raw_code_and_position() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields().copy(documentType = DocumentType("XY")),
            )
        val result = MrzValidator.validate(td3)
        val warning =
            result.warnings.filterIsInstance<MrzUnknownDocumentTypeCode>().singleOrNull()
                ?: error("Expected exactly one MrzUnknownDocumentTypeCode; got ${result.warnings}")
        assertEquals("XY", warning.rawCode)
        assertEquals(0, warning.position)
    }

    @Test
    fun empty_document_type_code_emits_MrzUnknownDocumentTypeCode_preserving_empty_raw_code() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields().copy(documentType = DocumentType("")),
            )
        val result = MrzValidator.validate(td3)
        val warning =
            result.warnings.filterIsInstance<MrzUnknownDocumentTypeCode>().singleOrNull()
                ?: error("Expected exactly one MrzUnknownDocumentTypeCode; got ${result.warnings}")
        assertEquals("", warning.rawCode)
        assertEquals(0, warning.position)
    }

    @Test
    fun all_starter_set_document_type_codes_do_not_emit_unknown_warning() {
        val starterSet = listOf("P", "V", "I", "PP", "PD", "PS")
        for (code in starterSet) {
            val td3 =
                specimenTd3(
                    commonFields =
                        specimenCommonFields().copy(documentType = DocumentType(code)),
                )
            val result = MrzValidator.validate(td3)
            assertTrue(
                result.warnings.none { it is MrzUnknownDocumentTypeCode },
                "Expected no MrzUnknownDocumentTypeCode warning for recognized code '$code'; got ${result.warnings}",
            )
        }
    }

    @Test
    fun unrecognized_document_type_code_does_not_block_other_validation() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(rawSex = '?', sex = Sex.UNSPECIFIED)
                        .copy(documentType = DocumentType("XY")),
            )
        val result = MrzValidator.validate(td3)
        assertTrue(
            result.warnings.any { it is MrzUnknownDocumentTypeCode },
            "Expected MrzUnknownDocumentTypeCode warning; got ${result.warnings}",
        )
        assertTrue(
            result.validationFailures.any { it is MrzInvalidSexValue },
            "Expected MrzInvalidSexValue failure to still be reported; got ${result.validationFailures}",
        )
    }

    // --- Unknown country code warning ---

    @Test
    fun unrecognized_issuing_state_emits_MrzUnknownCountryCode_with_field_raw_code_and_position() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields().copy(issuingState = CountryCode("ZZZ")),
            )
        val result = MrzValidator.validate(td3)
        val warning =
            result.warnings
                .filterIsInstance<MrzUnknownCountryCode>()
                .firstOrNull { it.field == MrzField.ISSUING_STATE }
                ?: error("Expected MrzUnknownCountryCode for ISSUING_STATE; got ${result.warnings}")
        assertEquals("ZZZ", warning.rawCode)
        assertEquals(2, warning.position)
    }

    @Test
    fun unrecognized_nationality_emits_MrzUnknownCountryCode_with_field_raw_code_and_position() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields().copy(nationality = CountryCode("ZZZ")),
            )
        val result = MrzValidator.validate(td3)
        val warning =
            result.warnings
                .filterIsInstance<MrzUnknownCountryCode>()
                .firstOrNull { it.field == MrzField.NATIONALITY }
                ?: error("Expected MrzUnknownCountryCode for NATIONALITY; got ${result.warnings}")
        assertEquals("ZZZ", warning.rawCode)
        assertEquals(54, warning.position)
    }

    @Test
    fun recognized_country_code_emits_no_unknown_warning_for_that_field() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields().copy(issuingState = CountryCode("DEU")),
            )
        val result = MrzValidator.validate(td3)
        assertTrue(
            result.warnings
                .filterIsInstance<MrzUnknownCountryCode>()
                .none { it.field == MrzField.ISSUING_STATE },
            "Recognized issuingState must not emit MrzUnknownCountryCode for ISSUING_STATE; got ${result.warnings}",
        )
    }

    @Test
    fun both_country_code_fields_emit_independent_warnings_when_both_unrecognized() {
        // Specimen has UTO for both fields by default; both warnings should fire.
        val result = MrzValidator.validate(specimenTd3())
        val warnings = result.warnings.filterIsInstance<MrzUnknownCountryCode>()
        assertEquals(
            setOf(MrzField.ISSUING_STATE, MrzField.NATIONALITY),
            warnings.map { it.field }.toSet(),
        )
        assertTrue(warnings.all { it.rawCode == "UTO" })
    }

    @Test
    fun empty_country_code_emits_MrzUnknownCountryCode_preserving_empty_raw_code() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields().copy(
                        issuingState = CountryCode(""),
                        nationality = CountryCode("DEU"),
                    ),
            )
        val result = MrzValidator.validate(td3)
        val warning =
            result.warnings
                .filterIsInstance<MrzUnknownCountryCode>()
                .firstOrNull { it.field == MrzField.ISSUING_STATE }
                ?: error("Expected MrzUnknownCountryCode for ISSUING_STATE; got ${result.warnings}")
        assertEquals("", warning.rawCode)
        assertEquals(2, warning.position)
    }

    // --- Name truncation warning ---

    @Test
    fun name_truncated_signal_emits_MrzNameTruncated_with_raw_field_and_position() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields().copy(
                        nameTruncated = true,
                        rawNameField = "VERYLONGPRIMARYNAME<<SECONDARYNAMEHERE0",
                    ),
            )
        val result = MrzValidator.validate(td3)
        val warning =
            result.warnings
                .filterIsInstance<MrzNameTruncated>()
                .singleOrNull()
                ?: error("Expected exactly one MrzNameTruncated; got ${result.warnings}")
        assertEquals("VERYLONGPRIMARYNAME<<SECONDARYNAMEHERE0", warning.rawNameField)
        assertEquals(5, warning.position)
    }

    @Test
    fun non_truncated_name_emits_no_MrzNameTruncated_warning() {
        // Specimen as-shipped: nameTruncated defaults to false in the fixture.
        val result = MrzValidator.validate(specimenTd3())
        assertTrue(
            result.warnings.none { it is MrzNameTruncated },
            "Non-truncated name must not emit MrzNameTruncated; got ${result.warnings}",
        )
    }

    @Test
    fun name_truncation_warning_does_not_block_other_validation() {
        val td3 =
            specimenTd3(
                commonFields =
                    specimenCommonFields(rawSex = '?', sex = Sex.UNSPECIFIED)
                        .copy(nameTruncated = true),
            )
        val result = MrzValidator.validate(td3)
        assertTrue(
            result.warnings.any { it is MrzNameTruncated },
            "Expected MrzNameTruncated warning; got ${result.warnings}",
        )
        assertTrue(
            result.validationFailures.any { it is MrzInvalidSexValue },
            "Expected MrzInvalidSexValue failure to still be reported; got ${result.validationFailures}",
        )
    }

    // TD1 validator coverage moved to MrzValidatorTd1Test once the TD1 parser slice landed.
}
