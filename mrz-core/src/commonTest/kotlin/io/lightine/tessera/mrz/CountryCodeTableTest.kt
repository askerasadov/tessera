package io.lightine.tessera.mrz

import io.lightine.tessera.domain.CountryCodeCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountryCodeTableTest {
    @Test
    fun lookup_returns_entry_for_starter_set_code_usa() {
        val entry = CountryCodeTable.lookup("USA")
        assertNotNull(entry)
        assertEquals("United States of America", entry.displayName)
        assertEquals(CountryCodeCategory.STATE, entry.category)
    }

    @Test
    fun lookup_returns_null_for_unrecognized_code() {
        assertNull(CountryCodeTable.lookup("ZZZ"))
    }

    @Test
    fun lookup_returns_null_for_synthetic_icao_test_code_uto() {
        // UTO ("Utopia") is the ICAO Doc 9303 synthetic test code; intentionally not a real country.
        assertNull(CountryCodeTable.lookup("UTO"))
    }

    @Test
    fun lookup_is_case_sensitive_per_icao_alphabet() {
        // MRZ alphabet is uppercase only; lowercase 'usa' is not a valid MRZ country code.
        assertNull(CountryCodeTable.lookup("usa"))
    }

    @Test
    fun all_returns_every_entry_in_the_starter_set() {
        val codes = CountryCodeTable.all().map { it.code }.toSet()
        assertEquals(setOf("USA", "GBR", "DEU", "FRA", "JPN"), codes)
    }

    @Test
    fun by_category_state_returns_all_starter_set_entries() {
        val stateCodes = CountryCodeTable.byCategory(CountryCodeCategory.STATE).map { it.code }.toSet()
        assertEquals(setOf("USA", "GBR", "DEU", "FRA", "JPN"), stateCodes)
    }

    @Test
    fun by_category_organization_returns_empty_list_in_starter_set() {
        // Starter set has no organization codes; this lock surfaces explicitly when populated.
        assertTrue(CountryCodeTable.byCategory(CountryCodeCategory.ORGANIZATION).isEmpty())
    }

    @Test
    fun by_category_stateless_returns_empty_list_in_starter_set() {
        assertTrue(CountryCodeTable.byCategory(CountryCodeCategory.STATELESS).isEmpty())
    }

    @Test
    fun by_category_refugee_returns_empty_list_in_starter_set() {
        assertTrue(CountryCodeTable.byCategory(CountryCodeCategory.REFUGEE).isEmpty())
    }

    @Test
    fun by_category_historical_returns_empty_list_in_starter_set() {
        assertTrue(CountryCodeTable.byCategory(CountryCodeCategory.HISTORICAL).isEmpty())
    }
}
