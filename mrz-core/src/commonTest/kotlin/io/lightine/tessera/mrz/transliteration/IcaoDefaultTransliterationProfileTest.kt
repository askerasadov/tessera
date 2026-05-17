package io.lightine.tessera.mrz.transliteration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class IcaoDefaultTransliterationProfileTest {
    @Test
    fun identifier_is_ICAO() {
        assertEquals("ICAO", IcaoDefaultTransliterationProfile.identifier)
        assertEquals("ICAO", IcaoDefaultTransliterationProfile.IDENTIFIER)
    }

    @Test
    fun empty_input_returns_empty_success() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("")
        assertEquals(TransliterationResult.Success(""), result)
    }

    @Test
    fun mrz_alphabet_passes_through_unchanged() {
        val input = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<"
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet(input)
        assertEquals(TransliterationResult.Success(input), result)
    }

    @Test
    fun lowercase_latin_uppercases() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("abcdefghijklmnopqrstuvwxyz")
        assertEquals(TransliterationResult.Success("ABCDEFGHIJKLMNOPQRSTUVWXYZ"), result)
    }

    @Test
    fun diacritics_use_no_expansion_convention() {
        // ä → A, ö → O, ü → U (not AE/OE/UE)
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Müller")
        assertEquals(TransliterationResult.Success("MULLER"), result)
    }

    @Test
    fun accented_e_strips_to_e() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("café")
        assertEquals(TransliterationResult.Success("CAFE"), result)
    }

    @Test
    fun n_with_tilde_strips_to_n() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("español")
        assertEquals(TransliterationResult.Success("ESPANOL"), result)
    }

    @Test
    fun aesc_ligature_expands_to_AE() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Æther")
        assertEquals(TransliterationResult.Success("AETHER"), result)
    }

    @Test
    fun oe_ligature_expands_to_OE() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Œuvre")
        assertEquals(TransliterationResult.Success("OEUVRE"), result)
    }

    @Test
    fun sharp_s_expands_to_SS() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Straße")
        assertEquals(TransliterationResult.Success("STRASSE"), result)
    }

    @Test
    fun thorn_expands_to_TH() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Þórr")
        assertEquals(TransliterationResult.Success("THORR"), result)
    }

    @Test
    fun eth_maps_to_D() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Ðagr")
        assertEquals(TransliterationResult.Success("DAGR"), result)
    }

    @Test
    fun ij_ligature_expands_to_IJ() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Ĳsselmeer")
        assertEquals(TransliterationResult.Success("IJSSELMEER"), result)
    }

    @Test
    fun schwa_maps_to_E_per_icao_default() {
        // ADR-009 explicitly calls out the schwa example: ICAO defaults to E,
        // while at least one issuing state uses A instead.
        assertEquals(
            TransliterationResult.Success("E"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("Ə"),
        )
        assertEquals(
            TransliterationResult.Success("E"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("ə"),
        )
    }

    @Test
    fun unmapped_character_falls_back_to_filler() {
        // Cyrillic letter outside the starter table.
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Я")
        assertEquals(TransliterationResult.Success("<"), result)
    }

    @Test
    fun space_falls_back_to_filler() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("A B")
        assertEquals(TransliterationResult.Success("A<B"), result)
    }

    @Test
    fun icao_default_never_returns_failure() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("anything including ✓ and ☃")
        assertIs<TransliterationResult.Success>(result)
    }

    @Test
    fun mixed_diacritic_and_expansion_in_one_input() {
        val result = IcaoDefaultTransliterationProfile.toMrzAlphabet("Müßig")
        assertEquals(TransliterationResult.Success("MUSSIG"), result)
    }
}
