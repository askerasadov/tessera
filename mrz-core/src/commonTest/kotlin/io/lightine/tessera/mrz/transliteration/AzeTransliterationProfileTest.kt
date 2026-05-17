package io.lightine.tessera.mrz.transliteration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AzeTransliterationProfileTest {
    @Test
    fun identifier_is_AZE() {
        assertEquals("AZE", AzeTransliterationProfile.identifier)
        assertEquals("AZE", AzeTransliterationProfile.IDENTIFIER)
    }

    @Test
    fun empty_input_returns_empty_success() {
        val result = AzeTransliterationProfile.toMrzAlphabet("")
        assertEquals(TransliterationResult.Success(""), result)
    }

    @Test
    fun mrz_alphabet_passes_through_unchanged() {
        val input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<"
        val result = AzeTransliterationProfile.toMrzAlphabet(input)
        assertEquals(TransliterationResult.Success(input), result)
    }

    @Test
    fun schwa_maps_to_A_diverging_from_icao() {
        // The load-bearing divergence from the ICAO default — same input,
        // different output by design.
        assertEquals(
            TransliterationResult.Success("A"),
            AzeTransliterationProfile.toMrzAlphabet("Ə"),
        )
        assertEquals(
            TransliterationResult.Success("A"),
            AzeTransliterationProfile.toMrzAlphabet("ə"),
        )
        assertEquals(
            TransliterationResult.Success("E"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("Ə"),
        )
    }

    @Test
    fun non_schwa_latin_diacritics_match_icao_behavior() {
        // Everything except schwa should produce the same output as ICAO default.
        val sample = "Müller café Straße Æther Œuvre Þórr"
        assertEquals(
            IcaoDefaultTransliterationProfile.toMrzAlphabet(sample),
            AzeTransliterationProfile.toMrzAlphabet(sample),
        )
    }

    @Test
    fun mixed_schwa_and_diacritic_uses_aze_schwa_override() {
        // A surname containing both a schwa and a standard diacritic;
        // the schwa override changes ə → A while ü stays U.
        val result = AzeTransliterationProfile.toMrzAlphabet("Müllər")
        assertEquals(TransliterationResult.Success("MULLAR"), result)
    }

    @Test
    fun unmapped_character_falls_back_to_filler() {
        // Cyrillic letter — outside the table for both profiles.
        val result = AzeTransliterationProfile.toMrzAlphabet("Я")
        assertEquals(TransliterationResult.Success("<"), result)
    }

    @Test
    fun aze_profile_never_returns_failure() {
        val result = AzeTransliterationProfile.toMrzAlphabet("anything including ✓ and ☃")
        assertIs<TransliterationResult.Success>(result)
    }

    @Test
    fun aze_is_pre_registered_in_registry() {
        val looked = TransliterationProfileRegistry.lookup(AzeTransliterationProfile.IDENTIFIER)
        assertEquals(AzeTransliterationProfile, looked)
    }
}
