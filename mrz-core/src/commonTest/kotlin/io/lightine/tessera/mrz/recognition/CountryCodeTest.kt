package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.types.vocabulary.CountryCodeCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountryCodeTest {
    @Test
    fun raw_code_is_preserved_verbatim_for_recognized_code() {
        val code = CountryCode("DEU")
        assertEquals("DEU", code.rawCode)
    }

    @Test
    fun raw_code_is_preserved_verbatim_for_unrecognized_code() {
        val code = CountryCode("ZZZ")
        assertEquals("ZZZ", code.rawCode)
    }

    @Test
    fun raw_code_is_preserved_verbatim_for_empty_code() {
        val code = CountryCode("")
        assertEquals("", code.rawCode)
    }

    @Test
    fun is_recognized_is_true_for_starter_set_member() {
        assertTrue(CountryCode("USA").isRecognized)
    }

    @Test
    fun is_recognized_is_false_for_code_outside_starter_set() {
        // XYZ is unassigned by ISO 3166-1 and not in any ICAO §5 part.
        assertTrue(!CountryCode("XYZ").isRecognized)
    }

    @Test
    fun is_recognized_is_true_for_icao_specimen_code_uto() {
        // UTO ("Utopia") is the ICAO Doc 9303 Part 3 §5 Part G specimen code;
        // categorized as OTHER (not a real country) but recognized.
        assertTrue(CountryCode("UTO").isRecognized)
        assertEquals(CountryCodeCategory.OTHER, CountryCode("UTO").category)
    }

    @Test
    fun display_name_is_present_for_recognized_code() {
        assertEquals("Germany", CountryCode("DEU").displayName)
    }

    @Test
    fun display_name_is_null_for_unrecognized_code() {
        assertNull(CountryCode("XYZ").displayName)
    }

    @Test
    fun category_is_state_for_starter_set_codes() {
        assertEquals(CountryCodeCategory.STATE, CountryCode("FRA").category)
    }

    @Test
    fun category_is_null_for_unrecognized_code() {
        assertNull(CountryCode("XYZ").category)
    }
}
