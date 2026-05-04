package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzCharacterSetViolation
import io.lightine.tessera.domain.MrzFormat
import io.lightine.tessera.domain.MrzInvalidLength
import io.lightine.tessera.domain.ReadMethod
import io.lightine.tessera.domain.Sex
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class MrzParserTest {
    private val ref2026 = Instant.parse("2026-05-04T12:00:00Z")

    private val specimenLine1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
    private val specimenLine2 = "L898902C<3UTO6908061F9406236ZE184226B<<<<<14"
    private val specimenLines = listOf(specimenLine1, specimenLine2)

    // --- Happy path ---

    @Test
    fun parses_specimen_td3_into_success_with_expected_common_fields() {
        val result = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        val success = assertIs<ParseResult.Success>(result)
        val td3 = assertIs<TD3>(success.document)

        assertEquals(MrzFormat.TD3, td3.format)
        assertEquals("P", td3.commonFields.documentType.rawCode)
        assertEquals("UTO", td3.commonFields.issuingState)
        assertEquals("L898902C<", td3.commonFields.documentNumber)
        assertEquals("UTO", td3.commonFields.nationality)
        assertEquals(Sex.FEMALE, td3.commonFields.sex)
        assertEquals("ERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<", td3.commonFields.rawNameField)
    }

    @Test
    fun parses_specimen_td3_into_success_with_expected_dates() {
        val result = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)

        // DOB 690806 with ref 2026 → SLIDING_WINDOW_BIRTH yields 1969
        assertEquals(1969, td3.commonFields.dateOfBirth.computedYear)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH, td3.commonFields.dateOfBirth.inferenceMethod)

        // Expiry 940623 with ref 2026 → falls outside [refYear-10, refYear+50] → RAW_ONLY
        // (locked behavior; see EXPIRY_PAST_WINDOW_YEARS in MrzDate)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, td3.commonFields.dateOfExpiry.inferenceMethod)
        assertEquals("94", td3.commonFields.dateOfExpiry.rawYear)
    }

    @Test
    fun parses_specimen_td3_into_success_with_expected_check_digits() {
        val result = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)
        val checks = td3.commonFields.checkDigits

        assertEquals('3', checks.documentNumber)
        assertEquals('1', checks.dateOfBirth)
        assertEquals('6', checks.dateOfExpiry)
        assertEquals('1', checks.optionalData)
        assertEquals('4', checks.composite)
    }

    @Test
    fun parses_specimen_td3_into_success_with_expected_personal_number() {
        val result = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)

        assertEquals("ZE184226B<<<<<", td3.personalNumber)
        assertEquals('1', td3.personalNumberCheckDigit)
    }

    @Test
    fun success_metadata_uses_backend_string_input_read_method() {
        val result = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        val success = assertIs<ParseResult.Success>(result)
        assertEquals(ReadMethod.BACKEND_STRING_INPUT, success.metadata.readMethod)
    }

    @Test
    fun success_metadata_has_empty_warnings_and_validation_failures_in_this_release() {
        val result = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        val success = assertIs<ParseResult.Success>(result)
        assertTrue(success.metadata.warnings.isEmpty())
        assertTrue(success.metadata.validationFailures.isEmpty())
    }

    @Test
    fun raw_lines_round_trip_through_the_parser_unchanged() {
        val result = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)
        assertEquals(specimenLines, td3.rawLines)
    }

    // --- Input forms ---

    @Test
    fun string_input_with_lf_line_separator_is_equivalent_to_list_input() {
        val joined = specimenLines.joinToString("\n")
        val fromString = MrzParser.parseTD3(joined, referenceTime = ref2026)
        val fromList = MrzParser.parseTD3(specimenLines, referenceTime = ref2026)
        assertEquals(fromList, fromString)
    }

    @Test
    fun string_input_with_crlf_line_separator_normalizes_to_same_result() {
        val joined = specimenLines.joinToString("\r\n")
        val result = MrzParser.parseTD3(joined, referenceTime = ref2026)
        assertIs<ParseResult.Success>(result)
    }

    @Test
    fun string_input_with_trailing_whitespace_is_accepted() {
        val joined = specimenLines.joinToString("\n") + "   \n  "
        val result = MrzParser.parseTD3(joined, referenceTime = ref2026)
        assertIs<ParseResult.Success>(result)
    }

    @Test
    fun string_input_with_leading_blank_lines_is_accepted() {
        val joined = "\n\n" + specimenLines.joinToString("\n")
        val result = MrzParser.parseTD3(joined, referenceTime = ref2026)
        assertIs<ParseResult.Success>(result)
    }

    // --- Format-specific field handling ---

    @Test
    fun parses_two_character_document_type_code_pp_into_recognized_document_type() {
        val line1 = "PPUTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
        val result = MrzParser.parseTD3(listOf(line1, specimenLine2), referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)

        assertEquals("PP", td3.commonFields.documentType.rawCode)
        assertTrue(td3.commonFields.documentType.isRecognized)
    }

    @Test
    fun parses_unrecognized_document_type_code_into_unrecognized_value_class() {
        val line1 = "ZZUTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
        val result = MrzParser.parseTD3(listOf(line1, specimenLine2), referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)

        assertEquals("ZZ", td3.commonFields.documentType.rawCode)
        assertTrue(!td3.commonFields.documentType.isRecognized)
    }

    @Test
    fun parses_male_sex_character() {
        val line2 = "L898902C<3UTO6908061M9406236ZE184226B<<<<<14"
        val result = MrzParser.parseTD3(listOf(specimenLine1, line2), referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)
        assertEquals(Sex.MALE, td3.commonFields.sex)
    }

    @Test
    fun parses_filler_sex_character_as_unspecified() {
        val line2 = "L898902C<3UTO6908061<9406236ZE184226B<<<<<14"
        val result = MrzParser.parseTD3(listOf(specimenLine1, line2), referenceTime = ref2026)
        val td3 = assertIs<TD3>(assertIs<ParseResult.Success>(result).document)
        assertEquals(Sex.UNSPECIFIED, td3.commonFields.sex)
    }

    // --- Error paths ---

    @Test
    fun fails_with_mrz_invalid_length_when_input_has_one_line() {
        val result = MrzParser.parseTD3(listOf(specimenLine1), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val error = assertIs<MrzInvalidLength>(failure.error)

        assertEquals(MrzFormat.TD3, error.format)
        assertEquals(2, error.expectedLineCount)
        assertEquals(1, error.observedLineCount)
    }

    @Test
    fun fails_with_mrz_invalid_length_when_a_line_is_too_short() {
        val shortLine2 = specimenLine2.dropLast(1)
        val result = MrzParser.parseTD3(listOf(specimenLine1, shortLine2), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val error = assertIs<MrzInvalidLength>(failure.error)

        assertEquals(listOf(44, 43), error.observedLineLengths)
    }

    @Test
    fun fails_with_mrz_invalid_length_for_empty_input() {
        val result = MrzParser.parseTD3("", referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        assertIs<MrzInvalidLength>(failure.error)
    }

    @Test
    fun fails_with_mrz_character_set_violation_for_lowercase_in_line_one() {
        val line1WithLowercase = "p<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
        val result = MrzParser.parseTD3(listOf(line1WithLowercase, specimenLine2), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val violation = assertIs<MrzCharacterSetViolation>(failure.error)

        assertEquals('p', violation.offendingCharacter)
        assertEquals(0, violation.position)
    }

    @Test
    fun mrz_character_set_violation_records_global_position_for_violation_in_line_two() {
        // Replace position 0 of line 2 with a space; global position = 44 + 0 = 44
        val line2WithSpace = " " + specimenLine2.drop(1)
        val result = MrzParser.parseTD3(listOf(specimenLine1, line2WithSpace), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val violation = assertIs<MrzCharacterSetViolation>(failure.error)

        assertEquals(' ', violation.offendingCharacter)
        assertEquals(44, violation.position)
    }

    @Test
    fun parser_does_not_bubble_illegal_argument_exception_from_check_digit_primitive() {
        // The translation-owed item from prior slices: lowercase input must surface as
        // MrzCharacterSetViolation, never as the IllegalArgumentException that
        // computeCheckDigit would throw for non-MRZ-alphabet input.
        val line1WithLowercase = "p<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
        try {
            val result = MrzParser.parseTD3(listOf(line1WithLowercase, specimenLine2), referenceTime = ref2026)
            assertIs<ParseResult.Failure>(result)
            assertIs<MrzCharacterSetViolation>((result as ParseResult.Failure).error)
        } catch (e: IllegalArgumentException) {
            fail("Parser should not bubble IllegalArgumentException for non-MRZ-alphabet input; got: ${e.message}")
        }
    }
}
