package io.lightine.tessera.mrz.formats

import kotlin.test.Test
import kotlin.test.assertEquals

class MrvBFormatSpecTest {
    @Test
    fun line_count_and_line_length_match_icao_doc_9303_part_7() {
        assertEquals(2, MrvBFormatSpec.lineCount)
        assertEquals(36, MrvBFormatSpec.lineLength)
    }

    @Test
    fun line_one_field_positions_match_icao_doc_9303_part_7() {
        assertEquals(FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2), MrvBFormatSpec.documentType)
        assertEquals(FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5), MrvBFormatSpec.issuingState)
        assertEquals(FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 36), MrvBFormatSpec.rawNameField)
    }

    @Test
    fun line_two_field_positions_match_icao_doc_9303_part_7() {
        assertEquals(FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 9), MrvBFormatSpec.documentNumber)
        assertEquals(FieldSpec(line = 1, startInLine = 9, endInLineExclusive = 10), MrvBFormatSpec.documentNumberCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 10, endInLineExclusive = 13), MrvBFormatSpec.nationality)
        assertEquals(FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19), MrvBFormatSpec.dateOfBirth)
        assertEquals(FieldSpec(line = 1, startInLine = 19, endInLineExclusive = 20), MrvBFormatSpec.dateOfBirthCheckDigit)
        assertEquals(FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21), MrvBFormatSpec.sex)
        assertEquals(FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 27), MrvBFormatSpec.dateOfExpiry)
        assertEquals(FieldSpec(line = 1, startInLine = 27, endInLineExclusive = 28), MrvBFormatSpec.dateOfExpiryCheckDigit)
        // Optional data spans the entire remainder of line 2 (8 chars). Like MRV-A, no per-field
        // check digit on optional data and no composite check digit.
        assertEquals(FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 36), MrvBFormatSpec.optionalData)
    }

    @Test
    fun global_position_of_line_one_field_equals_start_in_line() {
        assertEquals(0, MrvBFormatSpec.globalPositionOf(MrvBFormatSpec.documentType))
        assertEquals(2, MrvBFormatSpec.globalPositionOf(MrvBFormatSpec.issuingState))
        assertEquals(5, MrvBFormatSpec.globalPositionOf(MrvBFormatSpec.rawNameField))
    }

    @Test
    fun global_position_of_line_two_field_adds_line_length() {
        assertEquals(36, MrvBFormatSpec.globalPositionOf(MrvBFormatSpec.documentNumber))
        assertEquals(46, MrvBFormatSpec.globalPositionOf(MrvBFormatSpec.nationality))
        assertEquals(56, MrvBFormatSpec.globalPositionOf(MrvBFormatSpec.sex))
        assertEquals(64, MrvBFormatSpec.globalPositionOf(MrvBFormatSpec.optionalData))
    }
}
