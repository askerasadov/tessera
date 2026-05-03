package io.lightine.tessera.mrz.checkdigit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CheckDigitTest {
    @Test
    fun computes_check_digit_for_specimen_document_number_from_icao_doc_9303_part_4() {
        assertEquals('3', computeCheckDigit("L898902C<"))
    }

    @Test
    fun computes_check_digit_for_specimen_date_of_birth_from_icao_doc_9303_part_4() {
        assertEquals('1', computeCheckDigit("690806"))
    }

    @Test
    fun computes_check_digit_for_specimen_date_of_expiry_from_icao_doc_9303_part_4() {
        assertEquals('6', computeCheckDigit("940623"))
    }

    @Test
    fun applies_first_weight_of_seven_to_a_single_letter() {
        assertEquals('0', computeCheckDigit("A"))
    }

    @Test
    fun applies_first_weight_of_seven_to_a_single_digit() {
        assertEquals('7', computeCheckDigit("1"))
    }

    @Test
    fun returns_zero_for_empty_input() {
        assertEquals('0', computeCheckDigit(""))
    }

    @Test
    fun treats_filler_character_as_zero() {
        assertEquals('0', computeCheckDigit("<<<<<<"))
    }

    @Test
    fun rejects_lowercase_letter() {
        assertFailsWith<IllegalArgumentException> { computeCheckDigit("abc") }
    }

    @Test
    fun rejects_space() {
        assertFailsWith<IllegalArgumentException> { computeCheckDigit("AB CD") }
    }

    @Test
    fun rejects_punctuation() {
        assertFailsWith<IllegalArgumentException> { computeCheckDigit("AB!") }
    }
}
