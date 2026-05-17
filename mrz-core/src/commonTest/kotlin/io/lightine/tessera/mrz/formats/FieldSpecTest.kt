package io.lightine.tessera.mrz.formats

import kotlin.test.Test
import kotlin.test.assertEquals

class FieldSpecTest {
    @Test
    fun width_is_difference_between_start_and_end_exclusive() {
        assertEquals(2, FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2).width)
        assertEquals(6, FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19).width)
        assertEquals(1, FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21).width)
    }

    @Test
    fun extract_from_returns_substring_of_the_named_line() {
        val lines = listOf("ABCDE", "FGHIJ")
        assertEquals("BC", FieldSpec(line = 0, startInLine = 1, endInLineExclusive = 3).extractFrom(lines))
        assertEquals("FGHIJ", FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 5).extractFrom(lines))
    }

    @Test
    fun extract_char_from_returns_first_character_of_the_named_line_at_start_in_line() {
        val lines = listOf("ABCDE", "FGHIJ")
        assertEquals('B', FieldSpec(line = 0, startInLine = 1, endInLineExclusive = 2).extractCharFrom(lines))
        assertEquals('I', FieldSpec(line = 1, startInLine = 3, endInLineExclusive = 4).extractCharFrom(lines))
    }
}
