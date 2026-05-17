package io.lightine.tessera.mrz.formats

import kotlin.test.Test
import kotlin.test.assertEquals

class MrvAFormatSpecTest {
    @Test
    fun line_count_and_line_length_match_icao_doc_9303_part_7() {
        assertEquals(2, MrvAFormatSpec.lineCount)
        assertEquals(44, MrvAFormatSpec.lineLength)
    }

    @Test
    fun line_one_field_positions_match_icao_doc_9303_part_7() {
        assertEquals(FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2), MrvAFormatSpec.documentType)
        assertEquals(FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5), MrvAFormatSpec.issuingState)
        assertEquals(FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 44), MrvAFormatSpec.rawNameField)
    }

    @Test
    fun line_two_field_positions_match_icao_doc_9303_part_7() {
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 9), MrvAFormatSpec.documentNumber)
        assertEquals(FieldSpec(line = 1, startInLine = 9, endInLineExclusive = 10), MrvAFormatSpec.documentNumberCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 10, endInLineExclusive = 13), MrvAFormatSpec.nationality)
        assertEquals(FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19), MrvAFormatSpec.dateOfBirth)
        assertEquals(FieldSpec(line = 1, startInLine = 19, endInLineExclusive = 20), MrvAFormatSpec.dateOfBirthCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21), MrvAFormatSpec.sex)
        assertEquals(FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 27), MrvAFormatSpec.dateOfExpiry)
        assertEquals(FieldSpec(line = 1, startInLine = 27, endInLineExclusive = 28), MrvAFormatSpec.dateOfExpiryCheckDigit)
        // Optional data spans the entire remainder of line 2 (16 chars). Unlike TD3 and TD2,
        // there is no per-field check digit slot on optional data and no composite check digit.
        assertEquals(FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 44), MrvAFormatSpec.optionalData)
    }

    @Test
    fun global_position_of_line_one_field_equals_start_in_line() {
        assertEquals(0, MrvAFormatSpec.globalPositionOf(MrvAFormatSpec.documentType))
        assertEquals(2, MrvAFormatSpec.globalPositionOf(MrvAFormatSpec.issuingState))
        assertEquals(5, MrvAFormatSpec.globalPositionOf(MrvAFormatSpec.rawNameField))
    }

    @Test
    fun global_position_of_line_two_field_adds_line_length() {
        assertEquals(44, MrvAFormatSpec.globalPositionOf(MrvAFormatSpec.documentNumber))
        assertEquals(54, MrvAFormatSpec.globalPositionOf(MrvAFormatSpec.nationality))
        assertEquals(64, MrvAFormatSpec.globalPositionOf(MrvAFormatSpec.sex))
        assertEquals(72, MrvAFormatSpec.globalPositionOf(MrvAFormatSpec.optionalData))
    }
}
