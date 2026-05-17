package io.lightine.tessera.mrz.formats

import kotlin.test.Test
import kotlin.test.assertEquals

class Td1FormatSpecTest {
    @Test
    fun line_count_and_line_length_match_icao_doc_9303_part_5() {
        assertEquals(3, Td1FormatSpec.lineCount)
        assertEquals(30, Td1FormatSpec.lineLength)
    }

    @Test
    fun line_one_field_positions_match_icao_doc_9303_part_5() {
        assertEquals(FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2), Td1FormatSpec.documentType)
        assertEquals(FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5), Td1FormatSpec.issuingState)
        assertEquals(FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 14), Td1FormatSpec.documentNumber)
        assertEquals(FieldSpec(line = 0, startInLine = 14, endInLineExclusive = 15), Td1FormatSpec.documentNumberCheckDigit)
        assertEquals(FieldSpec(line = 0, startInLine = 15, endInLineExclusive = 30), Td1FormatSpec.optionalData1)
    }

    @Test
    fun line_two_field_positions_match_icao_doc_9303_part_5() {
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 6), Td1FormatSpec.dateOfBirth)
        assertEquals(FieldSpec(line = 1, startInLine = 6, endInLineExclusive = 7), Td1FormatSpec.dateOfBirthCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 7, endInLineExclusive = 8), Td1FormatSpec.sex)
        assertEquals(FieldSpec(line = 1, startInLine = 8, endInLineExclusive = 14), Td1FormatSpec.dateOfExpiry)
        assertEquals(FieldSpec(line = 1, startInLine = 14, endInLineExclusive = 15), Td1FormatSpec.dateOfExpiryCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 15, endInLineExclusive = 18), Td1FormatSpec.nationality)
        assertEquals(FieldSpec(line = 1, startInLine = 18, endInLineExclusive = 29), Td1FormatSpec.optionalData2)
        assertEquals(FieldSpec(line = 1, startInLine = 29, endInLineExclusive = 30), Td1FormatSpec.compositeCheckDigit)
    }

    @Test
    fun line_three_carries_the_name_field() {
        // TD1 places the name field on line 3 (full 30 chars), unlike TD3/TD2 which put it on line 1.
        assertEquals(FieldSpec(line = 2, startInLine = 0, endInLineExclusive = 30), Td1FormatSpec.rawNameField)
    }

    @Test
    fun composite_input_fields_concatenate_to_icao_doc_9303_part_5_composite_input() {
        // Composite check digit input per ICAO Doc 9303 Part 5: line 1 positions 6-30,
        // line 2 positions 1-7, 9-15, 19-29. Sex (line 2 idx 7), nationality (line 2 idx 15-18),
        // and the composite digit itself (line 2 idx 29) are excluded.
        assertEquals(4, Td1FormatSpec.compositeInputFields.size)
        assertEquals(FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 30), Td1FormatSpec.compositeInputFields[0])
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 7), Td1FormatSpec.compositeInputFields[1])
        assertEquals(FieldSpec(line = 1, startInLine = 8, endInLineExclusive = 15), Td1FormatSpec.compositeInputFields[2])
        assertEquals(FieldSpec(line = 1, startInLine = 18, endInLineExclusive = 29), Td1FormatSpec.compositeInputFields[3])
    }

    @Test
    fun global_position_of_line_one_field_equals_start_in_line() {
        assertEquals(0, Td1FormatSpec.globalPositionOf(Td1FormatSpec.documentType))
        assertEquals(5, Td1FormatSpec.globalPositionOf(Td1FormatSpec.documentNumber))
        assertEquals(14, Td1FormatSpec.globalPositionOf(Td1FormatSpec.documentNumberCheckDigit))
    }

    @Test
    fun global_position_of_line_two_field_adds_one_line_length() {
        assertEquals(30, Td1FormatSpec.globalPositionOf(Td1FormatSpec.dateOfBirth))
        assertEquals(37, Td1FormatSpec.globalPositionOf(Td1FormatSpec.sex))
        assertEquals(45, Td1FormatSpec.globalPositionOf(Td1FormatSpec.nationality))
        assertEquals(59, Td1FormatSpec.globalPositionOf(Td1FormatSpec.compositeCheckDigit))
    }

    @Test
    fun global_position_of_line_three_field_adds_two_line_lengths() {
        // Name field on line 3 → global position = 2 * 30 + 0 = 60
        assertEquals(60, Td1FormatSpec.globalPositionOf(Td1FormatSpec.rawNameField))
    }
}
