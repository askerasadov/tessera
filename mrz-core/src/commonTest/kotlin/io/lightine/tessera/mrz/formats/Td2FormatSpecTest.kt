package io.lightine.tessera.mrz.formats

import kotlin.test.Test
import kotlin.test.assertEquals

class Td2FormatSpecTest {
    @Test
    fun line_count_and_line_length_match_icao_doc_9303_part_6() {
        assertEquals(2, Td2FormatSpec.lineCount)
        assertEquals(36, Td2FormatSpec.lineLength)
    }

    @Test
    fun line_one_field_positions_match_icao_doc_9303_part_6() {
        assertEquals(FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2), Td2FormatSpec.documentType)
        assertEquals(FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5), Td2FormatSpec.issuingState)
        assertEquals(FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 36), Td2FormatSpec.rawNameField)
    }

    @Test
    fun line_two_field_positions_match_icao_doc_9303_part_6() {
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 9), Td2FormatSpec.documentNumber)
        assertEquals(FieldSpec(line = 1, startInLine = 9, endInLineExclusive = 10), Td2FormatSpec.documentNumberCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 10, endInLineExclusive = 13), Td2FormatSpec.nationality)
        assertEquals(FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19), Td2FormatSpec.dateOfBirth)
        assertEquals(FieldSpec(line = 1, startInLine = 19, endInLineExclusive = 20), Td2FormatSpec.dateOfBirthCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21), Td2FormatSpec.sex)
        assertEquals(FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 27), Td2FormatSpec.dateOfExpiry)
        assertEquals(FieldSpec(line = 1, startInLine = 27, endInLineExclusive = 28), Td2FormatSpec.dateOfExpiryCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 35), Td2FormatSpec.optionalData)
        assertEquals(FieldSpec(line = 1, startInLine = 35, endInLineExclusive = 36), Td2FormatSpec.compositeCheckDigit)
    }

    @Test
    fun composite_input_fields_concatenate_to_icao_doc_9303_part_6_composite_input() {
        // Composite check digit input per ICAO Doc 9303 Part 6: doc number + its check digit,
        // DOB + its check digit, expiry + its check digit + optional data (no per-field check
        // digit on optional data in TD2). Sex (position 21) and the composite digit itself
        // (position 36) are excluded.
        assertEquals(3, Td2FormatSpec.compositeInputFields.size)
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 10), Td2FormatSpec.compositeInputFields[0])
        assertEquals(FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 20), Td2FormatSpec.compositeInputFields[1])
        assertEquals(FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 35), Td2FormatSpec.compositeInputFields[2])
    }

    @Test
    fun global_position_of_line_one_field_equals_start_in_line() {
        assertEquals(0, Td2FormatSpec.globalPositionOf(Td2FormatSpec.documentType))
        assertEquals(2, Td2FormatSpec.globalPositionOf(Td2FormatSpec.issuingState))
        assertEquals(5, Td2FormatSpec.globalPositionOf(Td2FormatSpec.rawNameField))
    }

    @Test
    fun global_position_of_line_two_field_adds_line_length() {
        assertEquals(36, Td2FormatSpec.globalPositionOf(Td2FormatSpec.documentNumber))
        assertEquals(46, Td2FormatSpec.globalPositionOf(Td2FormatSpec.nationality))
        assertEquals(56, Td2FormatSpec.globalPositionOf(Td2FormatSpec.sex))
        assertEquals(71, Td2FormatSpec.globalPositionOf(Td2FormatSpec.compositeCheckDigit))
    }
}
