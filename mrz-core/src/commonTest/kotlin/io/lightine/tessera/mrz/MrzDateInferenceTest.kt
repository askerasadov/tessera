package io.lightine.tessera.mrz

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MrzDateInferenceTest {
    private val ref2026 = Instant.parse("2026-05-04T12:00:00Z")

    @Test
    fun direct_construction_with_raw_only_defaults_to_raw_only_inference() {
        val date = MrzDate(rawYear = "69", rawMonth = "08", rawDay = "06")
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
        assertNull(date.computedYear)
        assertNull(date.computedDate)
    }

    // --- parseBirth ---

    @Test
    fun parse_birth_picks_two_thousand_century_when_year_is_in_recent_past() {
        val date = MrzDate.parseBirth(rawYear = "20", rawMonth = "03", rawDay = "15", referenceTime = ref2026)
        assertEquals(2020, date.computedYear)
        assertEquals(LocalDate(2020, 3, 15), date.computedDate)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH, date.inferenceMethod)
    }

    @Test
    fun parse_birth_picks_two_thousand_century_when_birth_date_is_before_reference_date_in_same_year() {
        val date = MrzDate.parseBirth(rawYear = "26", rawMonth = "01", rawDay = "10", referenceTime = ref2026)
        assertEquals(2026, date.computedYear)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH, date.inferenceMethod)
    }

    @Test
    fun parse_birth_falls_back_to_nineteen_hundred_century_when_two_thousand_century_is_in_future() {
        val date = MrzDate.parseBirth(rawYear = "80", rawMonth = "06", rawDay = "15", referenceTime = ref2026)
        assertEquals(1980, date.computedYear)
        assertEquals(LocalDate(1980, 6, 15), date.computedDate)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH, date.inferenceMethod)
    }

    @Test
    fun parse_birth_falls_back_to_nineteen_hundred_century_when_birth_date_in_same_year_is_after_reference_date() {
        val date = MrzDate.parseBirth(rawYear = "26", rawMonth = "11", rawDay = "20", referenceTime = ref2026)
        assertEquals(1926, date.computedYear)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH, date.inferenceMethod)
    }

    @Test
    fun parse_birth_picks_specimen_passport_birth_year() {
        val date = MrzDate.parseBirth(rawYear = "69", rawMonth = "08", rawDay = "06", referenceTime = ref2026)
        assertEquals(1969, date.computedYear)
        assertEquals(LocalDate(1969, 8, 6), date.computedDate)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH, date.inferenceMethod)
    }

    @Test
    fun parse_birth_returns_raw_only_for_non_digit_year() {
        val date = MrzDate.parseBirth(rawYear = "ab", rawMonth = "08", rawDay = "06", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
        assertNull(date.computedYear)
        assertNull(date.computedDate)
        assertEquals("ab", date.rawYear)
    }

    @Test
    fun parse_birth_returns_raw_only_for_month_thirteen() {
        val date = MrzDate.parseBirth(rawYear = "90", rawMonth = "13", rawDay = "01", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    @Test
    fun parse_birth_returns_raw_only_for_day_thirty_two() {
        val date = MrzDate.parseBirth(rawYear = "90", rawMonth = "01", rawDay = "32", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    @Test
    fun parse_birth_returns_raw_only_for_february_thirty() {
        val date = MrzDate.parseBirth(rawYear = "90", rawMonth = "02", rawDay = "30", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    @Test
    fun parse_birth_returns_raw_only_for_feb_29_when_neither_century_is_a_leap_year() {
        // 2001 and 1901 are both non-leap years
        val date = MrzDate.parseBirth(rawYear = "01", rawMonth = "02", rawDay = "29", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    @Test
    fun parse_birth_returns_raw_only_for_wrong_length_year_field() {
        val date = MrzDate.parseBirth(rawYear = "123", rawMonth = "08", rawDay = "06", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    // --- parseExpiry ---

    @Test
    fun parse_expiry_picks_two_thousand_century_for_recent_past_year() {
        val date = MrzDate.parseExpiry(rawYear = "20", rawMonth = "03", rawDay = "15", referenceTime = ref2026)
        assertEquals(2020, date.computedYear)
        assertEquals(LocalDate(2020, 3, 15), date.computedDate)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_EXPIRY, date.inferenceMethod)
    }

    @Test
    fun parse_expiry_picks_two_thousand_century_for_near_future_year() {
        val date = MrzDate.parseExpiry(rawYear = "30", rawMonth = "06", rawDay = "01", referenceTime = ref2026)
        assertEquals(2030, date.computedYear)
        assertEquals(MrzDateInferenceMethod.SLIDING_WINDOW_EXPIRY, date.inferenceMethod)
    }

    @Test
    fun parse_expiry_returns_raw_only_when_no_century_falls_within_the_plausibility_window() {
        // YY=80 with ref=2026: 2080 is beyond +50 future, 1980 is beyond -10 past
        val date = MrzDate.parseExpiry(rawYear = "80", rawMonth = "06", rawDay = "01", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
        assertNull(date.computedYear)
    }

    @Test
    fun parse_expiry_returns_raw_only_for_year_at_extreme_future() {
        // YY=99 with ref=2026: 2099 is 73 years out (beyond +50), 1999 is 27 years past (beyond -10)
        val date = MrzDate.parseExpiry(rawYear = "99", rawMonth = "12", rawDay = "31", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    @Test
    fun parse_expiry_picks_specimen_passport_expiry_year() {
        // The 2026-era reference makes the 1994 specimen expiry RAW_ONLY: 1994 is beyond the
        // EXPIRY_PAST_WINDOW of 10 years. The point of this test is to lock that behavior so any
        // future widening of the past window surfaces explicitly.
        val date = MrzDate.parseExpiry(rawYear = "94", rawMonth = "06", rawDay = "23", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    @Test
    fun parse_expiry_returns_raw_only_for_non_digit_year() {
        val date = MrzDate.parseExpiry(rawYear = "xx", rawMonth = "06", rawDay = "01", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }

    @Test
    fun parse_expiry_returns_raw_only_for_invalid_calendar_date() {
        val date = MrzDate.parseExpiry(rawYear = "30", rawMonth = "13", rawDay = "01", referenceTime = ref2026)
        assertEquals(MrzDateInferenceMethod.RAW_ONLY, date.inferenceMethod)
    }
}
