package io.lightine.tessera.mrz.parsing

import io.lightine.tessera.domain.errors.MrzCharacterSetViolation
import io.lightine.tessera.domain.errors.MrzCheckDigitMismatch
import io.lightine.tessera.domain.errors.MrzInvalidLength
import io.lightine.tessera.domain.errors.MrzInvalidSexValue
import io.lightine.tessera.domain.errors.MrzUnknownCountryCode
import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat
import io.lightine.tessera.domain.vocabulary.ReadMethod
import io.lightine.tessera.domain.vocabulary.Sex
import io.lightine.tessera.mrz.model.TD1
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MrzParserTd1Test {
    private val ref2026 = Instant.parse("2026-05-04T12:00:00Z")

    // Synthetic TD1 specimen — same Anna Eriksson persona used across the other format
    // fixtures, adapted to a TD1 ID card. ICAO Doc 9303 Part 5: 3 lines x 30 chars. Document
    // type `I` (identity card). Doc number L898902C<; DOB 690806; expiry 300806; sex F.
    // Optional data 1 (line 1, 15 chars) and optional data 2 (line 2, 11 chars) are all filler.
    // Per-field check digits and composite computed via the SDK's own algorithm and locked here.
    private val specimenLine1 = "I<UTOL898902C<3<<<<<<<<<<<<<<<"
    private val specimenLine2 = "6908061F3008063UTO<<<<<<<<<<<2"
    private val specimenLine3 = "ERIKSSON<<ANNA<MARIA<<<<<<<<<<"
    private val specimenLines = listOf(specimenLine1, specimenLine2, specimenLine3)

    // --- Happy path ---

    @Test
    fun parses_specimen_td1_into_success_with_expected_common_fields() {
        val result = MrzParser.parseTD1(specimenLines, referenceTime = ref2026)
        val success = assertIs<ParseResult.Success>(result)
        val td1 = assertIs<TD1>(success.document)

        assertEquals(MrzFormat.TD1, td1.format)
        assertEquals("I", td1.commonFields.documentType.rawCode)
        assertTrue(td1.commonFields.documentType.isRecognized)
        assertEquals("UTO", td1.commonFields.issuingState.rawCode)
        assertEquals("L898902C<", td1.commonFields.documentNumber)
        assertEquals("UTO", td1.commonFields.nationality.rawCode)
        assertEquals(Sex.FEMALE, td1.commonFields.sex)
        assertEquals("ERIKSSON<<ANNA<MARIA<<<<<<<<<<", td1.commonFields.rawNameField)
        assertEquals("ERIKSSON", td1.commonFields.primaryIdentifier)
        assertEquals("ANNA MARIA", td1.commonFields.secondaryIdentifier)
        assertEquals(false, td1.commonFields.nameTruncated)
    }

    @Test
    fun parses_specimen_td1_into_success_with_expected_check_digits() {
        val result = MrzParser.parseTD1(specimenLines, referenceTime = ref2026)
        val td1 = assertIs<TD1>(assertIs<ParseResult.Success>(result).document)
        val checks = td1.commonFields.checkDigits

        assertEquals('3', checks.documentNumber)
        assertEquals('1', checks.dateOfBirth)
        assertEquals('3', checks.dateOfExpiry)
        assertNull(checks.optionalData, "TD1 has no per-field check digit on either optional data slot")
        assertEquals('2', checks.composite)
    }

    @Test
    fun parses_specimen_td1_into_success_with_expected_optional_data_slots() {
        val result = MrzParser.parseTD1(specimenLines, referenceTime = ref2026)
        val td1 = assertIs<TD1>(assertIs<ParseResult.Success>(result).document)
        // TD1 has two optional-data slots: 15 chars on line 1 (after the doc-number check digit)
        // and 11 chars on line 2 (between nationality and composite check digit).
        assertEquals("<<<<<<<<<<<<<<<", td1.optionalData1)
        assertEquals(15, td1.optionalData1.length)
        assertEquals("<<<<<<<<<<<", td1.optionalData2)
        assertEquals(11, td1.optionalData2.length)
    }

    @Test
    fun success_metadata_uses_backend_string_input_read_method() {
        val result = MrzParser.parseTD1(specimenLines, referenceTime = ref2026)
        val success = assertIs<ParseResult.Success>(result)
        assertEquals(ReadMethod.BACKEND_STRING_INPUT, success.metadata.readMethod)
    }

    @Test
    fun success_metadata_has_no_validation_failures_for_clean_specimen() {
        // "I" is in the SDK's deliberate starter set; only the UTO country warnings fire.
        val result = MrzParser.parseTD1(specimenLines, referenceTime = ref2026)
        val success = assertIs<ParseResult.Success>(result)
        assertTrue(success.metadata.validationFailures.isEmpty())
        val unknownCountryWarnings = success.metadata.warnings.filterIsInstance<MrzUnknownCountryCode>()
        assertEquals(2, unknownCountryWarnings.size)
        assertEquals(setOf(MrzField.ISSUING_STATE, MrzField.NATIONALITY), unknownCountryWarnings.map { it.field }.toSet())
    }

    @Test
    fun raw_lines_round_trip_through_the_parser_unchanged() {
        val result = MrzParser.parseTD1(specimenLines, referenceTime = ref2026)
        val td1 = assertIs<TD1>(assertIs<ParseResult.Success>(result).document)
        assertEquals(specimenLines, td1.rawLines)
    }

    // --- Input forms ---

    @Test
    fun string_input_with_lf_line_separator_is_equivalent_to_list_input() {
        val joined = specimenLines.joinToString("\n")
        val fromString = MrzParser.parseTD1(joined, referenceTime = ref2026)
        val fromList = MrzParser.parseTD1(specimenLines, referenceTime = ref2026)
        assertEquals(fromList, fromString)
    }

    // --- Validation wiring ---

    @Test
    fun returns_partial_success_with_check_digit_mismatch_when_document_number_check_digit_is_corrupted() {
        // Flip docCheck '3' to '0' at position 14 of line 1.
        val corruptedLine1 = "I<UTOL898902C<0<<<<<<<<<<<<<<<"
        val result = MrzParser.parseTD1(listOf(corruptedLine1, specimenLine2, specimenLine3), referenceTime = ref2026)
        val partial = assertIs<ParseResult.PartialSuccess>(result)

        val mismatch =
            partial.metadata.validationFailures
                .filterIsInstance<MrzCheckDigitMismatch>()
                .firstOrNull { it.field == MrzField.DOCUMENT_NUMBER }
        assertTrue(mismatch != null, "Expected MrzCheckDigitMismatch for DOCUMENT_NUMBER; got ${partial.metadata.validationFailures}")
        assertEquals('3', mismatch.expected)
        assertEquals('0', mismatch.observed)
        // TD1 doc number check digit position: line 1 index 14, global = 14
        assertEquals(14, mismatch.position)
    }

    @Test
    fun returns_partial_success_with_composite_check_digit_mismatch_when_optional_data_is_corrupted() {
        // TD1 has no per-field check digit on either optional-data slot — only the composite
        // covers them. Corrupting optional data 1 invalidates the composite digit ONLY.
        val corruptedLine1 = "I<UTOL898902C<3ABCDEFGHIJKLMNO"
        val result = MrzParser.parseTD1(listOf(corruptedLine1, specimenLine2, specimenLine3), referenceTime = ref2026)
        val partial = assertIs<ParseResult.PartialSuccess>(result)

        val compositeMismatch =
            partial.metadata.validationFailures
                .filterIsInstance<MrzCheckDigitMismatch>()
                .firstOrNull { it.field == MrzField.COMPOSITE }
        assertTrue(compositeMismatch != null, "Expected MrzCheckDigitMismatch for COMPOSITE; got ${partial.metadata.validationFailures}")
        assertEquals('2', compositeMismatch.observed)

        val optionalDataMismatch =
            partial.metadata.validationFailures
                .filterIsInstance<MrzCheckDigitMismatch>()
                .firstOrNull { it.field == MrzField.OPTIONAL_DATA }
        assertNull(optionalDataMismatch, "TD1 has no OPTIONAL_DATA check digit; the validator must not produce a per-field mismatch for it")
    }

    @Test
    fun returns_partial_success_with_invalid_sex_value_when_sex_character_is_not_in_allowed_set() {
        val line2WithInvalidSex = "6908061Q3008063UTO<<<<<<<<<<<2"
        val result = MrzParser.parseTD1(listOf(specimenLine1, line2WithInvalidSex, specimenLine3), referenceTime = ref2026)
        val partial = assertIs<ParseResult.PartialSuccess>(result)
        val td1 = assertIs<TD1>(partial.document)

        assertEquals(Sex.UNSPECIFIED, td1.commonFields.sex)
        assertEquals('Q', td1.commonFields.rawSex)

        val invalid =
            partial.metadata.validationFailures
                .filterIsInstance<MrzInvalidSexValue>()
                .firstOrNull()
        assertTrue(invalid != null)
        assertEquals('Q', invalid.observed)
        // TD1 sex position: line 2 index 7, global = 30 + 7 = 37
        assertEquals(37, invalid.position)
    }

    // --- Error paths ---

    @Test
    fun fails_with_mrz_invalid_length_when_input_has_two_lines_instead_of_three() {
        val result = MrzParser.parseTD1(listOf(specimenLine1, specimenLine2), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val error = assertIs<MrzInvalidLength>(failure.error)

        assertEquals(MrzFormat.TD1, error.format)
        assertEquals(3, error.expectedLineCount)
        assertEquals(30, error.expectedLineLength)
        assertEquals(2, error.observedLineCount)
    }

    @Test
    fun fails_with_mrz_invalid_length_when_lines_have_td3_length_instead_of_td1_length() {
        // TD1 is 3 x 30. Passing 2 x 44 TD3-shape input must fail (line count and line length).
        val td3Line1 = "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<"
        val td3Line2 = "L898902C<3UTO6908061F9406236ZE184226B<<<<<14"
        val result = MrzParser.parseTD1(listOf(td3Line1, td3Line2), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val error = assertIs<MrzInvalidLength>(failure.error)
        assertEquals(MrzFormat.TD1, error.format)
        assertEquals(30, error.expectedLineLength)
        assertEquals(listOf(44, 44), error.observedLineLengths)
    }

    @Test
    fun fails_with_mrz_character_set_violation_for_lowercase_in_line_one() {
        val line1WithLowercase = "i<UTOL898902C<3<<<<<<<<<<<<<<<"
        val result = MrzParser.parseTD1(listOf(line1WithLowercase, specimenLine2, specimenLine3), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val violation = assertIs<MrzCharacterSetViolation>(failure.error)
        assertEquals('i', violation.offendingCharacter)
        assertEquals(0, violation.position)
    }

    @Test
    fun mrz_character_set_violation_in_line_three_records_global_position() {
        // Replace position 0 of line 3 with a space; global position = 2 * 30 + 0 = 60.
        val line3WithSpace = " " + specimenLine3.drop(1)
        val result = MrzParser.parseTD1(listOf(specimenLine1, specimenLine2, line3WithSpace), referenceTime = ref2026)
        val failure = assertIs<ParseResult.Failure>(result)
        val violation = assertIs<MrzCharacterSetViolation>(failure.error)
        assertEquals(' ', violation.offendingCharacter)
        assertEquals(60, violation.position)
    }
}
