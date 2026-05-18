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
    fun schwa_falls_through_to_filler_per_icao_annex_g_absence() {
        // ICAO Doc 9303 Part 3 Annex G's Latin table covers U+00C0-00DE,
        // U+0100-017D, and U+1E9E. The Latin schwa (U+018F / U+0259) is in
        // Latin Extended-B (U+0180-024F), entirely outside Annex G's scope.
        // The ICAO default profile therefore falls through to filler.
        // Country-specific profiles (e.g., AzeTransliterationProfile) apply
        // their own override per ADR-009.
        assertEquals(
            TransliterationResult.Success("<"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("Ə"),
        )
        assertEquals(
            TransliterationResult.Success("<"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("ə"),
        )
    }

    @Test
    fun apostrophe_is_omitted_per_icao_part_3_section_4_6() {
        // Spec: "Apostrophe: This shall be omitted; name components separated by
        // the apostrophe shall be combined, and no filler character shall be
        // inserted in its place in the MRZ. Example VIZ: D'ARTAGNAN MRZ: DARTAGNAN."
        assertEquals(
            TransliterationResult.Success("DARTAGNAN"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("D'ARTAGNAN"),
        )
        assertEquals(
            TransliterationResult.Success("OCONNOR"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("O'CONNOR"),
        )
        // Right single quotation mark (U+2019) — commonly typed apostrophe variant.
        assertEquals(
            TransliterationResult.Success("DARTAGNAN"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("D’ARTAGNAN"),
        )
    }

    @Test
    fun common_punctuation_is_omitted_per_icao_part_3_section_4_6() {
        // Spec: "All other punctuation characters shall be omitted from the MRZ
        // (no filler character shall be inserted in their place in the MRZ)."
        assertEquals(
            TransliterationResult.Success("ABC"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("A.B,C"),
        )
        assertEquals(
            TransliterationResult.Success("AB"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("A!B?"),
        )
        assertEquals(
            TransliterationResult.Success("AB"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("A(B)"),
        )
    }

    @Test
    fun hyphen_falls_through_to_filler_per_icao_part_3_section_4_6() {
        // Spec: "Where a hyphen appears between two name components, it shall
        // be represented in the MRZ by a single filler character (<)."
        // Hyphen is intentionally NOT in the punctuation-omitted set; it uses
        // the unmapped-character fallback to filler.
        assertEquals(
            TransliterationResult.Success("MARIE<ELISE"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("MARIE-ELISE"),
        )
        assertEquals(
            TransliterationResult.Success("SMITH<JONES"),
            IcaoDefaultTransliterationProfile.toMrzAlphabet("SMITH-JONES"),
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
