package io.lightine.tessera.mrz.formats

import kotlin.test.Test
import kotlin.test.assertEquals

class Td3FormatSpecTest {
    @Test
    fun line_count_and_line_length_match_icao_doc_9303_part_4() {
        assertEquals(2, Td3FormatSpec.lineCount)
        assertEquals(44, Td3FormatSpec.lineLength)
    }

    @Test
    fun line_one_field_positions_match_icao_doc_9303_part_4() {
        assertEquals(FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2), Td3FormatSpec.documentType)
        assertEquals(FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5), Td3FormatSpec.issuingState)
        assertEquals(FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 44), Td3FormatSpec.rawNameField)
    }

    @Test
    fun line_two_field_positions_match_icao_doc_9303_part_4() {
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 9), Td3FormatSpec.documentNumber)
        assertEquals(FieldSpec(line = 1, startInLine = 9, endInLineExclusive = 10), Td3FormatSpec.documentNumberCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 10, endInLineExclusive = 13), Td3FormatSpec.nationality)
        assertEquals(FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19), Td3FormatSpec.dateOfBirth)
        assertEquals(FieldSpec(line = 1, startInLine = 19, endInLineExclusive = 20), Td3FormatSpec.dateOfBirthCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21), Td3FormatSpec.sex)
        assertEquals(FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 27), Td3FormatSpec.dateOfExpiry)
        assertEquals(FieldSpec(line = 1, startInLine = 27, endInLineExclusive = 28), Td3FormatSpec.dateOfExpiryCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 42), Td3FormatSpec.personalNumber)
        assertEquals(FieldSpec(line = 1, startInLine = 42, endInLineExclusive = 43), Td3FormatSpec.personalNumberCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 43, endInLineExclusive = 44), Td3FormatSpec.compositeCheckDigit)
    }

    @Test
    fun composite_input_fields_concatenate_to_icao_doc_9303_part_4_composite_input() {
        // Composite check digit input per ICAO Doc 9303 Part 4: doc number + its check digit,
        // DOB + its check digit, expiry + its check digit + personal number + its check digit.
        // Sex (position 20) and the composite digit itself (position 43) are excluded.
        assertEquals(3, Td3FormatSpec.compositeInputFields.size)
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 10), Td3FormatSpec.compositeInputFields[0])
        assertEquals(FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 20), Td3FormatSpec.compositeInputFields[1])
        assertEquals(FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 43), Td3FormatSpec.compositeInputFields[2])
    }

    @Test
    fun global_position_of_line_one_field_equals_start_in_line() {
        assertEquals(0, Td3FormatSpec.globalPositionOf(Td3FormatSpec.documentType))
        assertEquals(2, Td3FormatSpec.globalPositionOf(Td3FormatSpec.issuingState))
        assertEquals(5, Td3FormatSpec.globalPositionOf(Td3FormatSpec.rawNameField))
    }

    @Test
    fun global_position_of_line_two_field_adds_line_length() {
        assertEquals(44, Td3FormatSpec.globalPositionOf(Td3FormatSpec.documentNumber))
        assertEquals(54, Td3FormatSpec.globalPositionOf(Td3FormatSpec.nationality))
        assertEquals(64, Td3FormatSpec.globalPositionOf(Td3FormatSpec.sex))
        assertEquals(87, Td3FormatSpec.globalPositionOf(Td3FormatSpec.compositeCheckDigit))
    }
}
