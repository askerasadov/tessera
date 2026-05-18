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
    fun schwa_maps_to_A_via_bgn_pcgn_and_icao_chain() {
        // The load-bearing override per ADR-009 + Phase 4 verification:
        // BGN/PCGN 1993 Agreement (UK govt, 2022) Note 1 says schwa's fallback
        // when unreproducible is Ä; ICAO Annex G under no-expansion maps Ä → A.
        // The chained substitution Ə → Ä → A matches observed AZE practice.
        assertEquals(
            TransliterationResult.Success("A"),
            AzeTransliterationProfile.toMrzAlphabet("Ə"),
        )
        assertEquals(
            TransliterationResult.Success("A"),
            AzeTransliterationProfile.toMrzAlphabet("ə"),
        )
        // ICAO default has no schwa entry — falls through to filler (see
        // IcaoDefaultTransliterationProfileTest for full reasoning).
        assertEquals(
            TransliterationResult.Success("<"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("Ə"),
        )
    }

    @Test
    fun aze_alphabet_letters_each_map_per_icao_or_aze_override() {
        // Per BGN/PCGN 1993 Agreement Note 4 (UK government, 2022 revision),
        // the AZE Latin alphabet uses these letters beyond basic ASCII A-Z:
        // Ç Ğ İ ı Ö Ş Ü (covered by ICAO Annex G under no-expansion) and
        // Ə (covered by the AZE schwa override per the BGN/PCGN + ICAO chain).
        //
        // Phase 4 finding F24: verify each maps correctly after the PR 4
        // Latin expansion. Covered character-by-character to lock the
        // expected behaviour against any future table change.
        assertEquals(TransliterationResult.Success("C"), AzeTransliterationProfile.toMrzAlphabet("Ç"))
        assertEquals(TransliterationResult.Success("C"), AzeTransliterationProfile.toMrzAlphabet("ç"))
        assertEquals(TransliterationResult.Success("G"), AzeTransliterationProfile.toMrzAlphabet("Ğ"))
        assertEquals(TransliterationResult.Success("G"), AzeTransliterationProfile.toMrzAlphabet("ğ"))
        assertEquals(TransliterationResult.Success("I"), AzeTransliterationProfile.toMrzAlphabet("İ"))
        assertEquals(TransliterationResult.Success("I"), AzeTransliterationProfile.toMrzAlphabet("ı"))
        assertEquals(TransliterationResult.Success("O"), AzeTransliterationProfile.toMrzAlphabet("Ö"))
        assertEquals(TransliterationResult.Success("O"), AzeTransliterationProfile.toMrzAlphabet("ö"))
        assertEquals(TransliterationResult.Success("S"), AzeTransliterationProfile.toMrzAlphabet("Ş"))
        assertEquals(TransliterationResult.Success("S"), AzeTransliterationProfile.toMrzAlphabet("ş"))
        assertEquals(TransliterationResult.Success("U"), AzeTransliterationProfile.toMrzAlphabet("Ü"))
        assertEquals(TransliterationResult.Success("U"), AzeTransliterationProfile.toMrzAlphabet("ü"))
        assertEquals(TransliterationResult.Success("A"), AzeTransliterationProfile.toMrzAlphabet("Ə"))
        assertEquals(TransliterationResult.Success("A"), AzeTransliterationProfile.toMrzAlphabet("ə"))
        // Q and X are basic ASCII and pass through unchanged.
        assertEquals(TransliterationResult.Success("Q"), AzeTransliterationProfile.toMrzAlphabet("Q"))
        assertEquals(TransliterationResult.Success("X"), AzeTransliterationProfile.toMrzAlphabet("X"))
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
