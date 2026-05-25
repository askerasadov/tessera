package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.types.vocabulary.UnmappedCharacter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TransliterateTest {
    @Test
    fun outcome_carries_profile_identifier_original_and_normalized_forms() {
        val outcome = IcaoDefaultTransliterationProfile.transliterate("café")
        assertEquals(IcaoDefaultTransliterationProfile.IDENTIFIER, outcome.profileIdentifier)
        assertEquals("café", outcome.originalInput)
        // Already-NFC input passes through normalization unchanged.
        assertEquals("café", outcome.normalizedInput)
    }

    @Test
    fun outcome_normalizes_nfd_input_before_applying_profile() {
        val decomposed = "café" // e + combining acute (NFD)
        val outcome = IcaoDefaultTransliterationProfile.transliterate(decomposed)
        // Normalized form is precomposed NFC; transliteration output is MRZ alphabet.
        assertEquals("café", outcome.normalizedInput)
        val result = assertIs<TransliterationResult.Success>(outcome.result)
        assertEquals("CAFE", result.output)
    }

    @Test
    fun outcome_carries_success_result_for_icao_default() {
        val outcome = IcaoDefaultTransliterationProfile.transliterate("Müller")
        val result = assertIs<TransliterationResult.Success>(outcome.result)
        assertEquals("MULLER", result.output)
    }

    @Test
    fun outcome_carries_failure_result_for_profile_with_no_fallback() {
        val profile =
            object : TransliterationProfile {
                override val identifier: String = "TEST-FAIL-ONLY"

                override fun toMrzAlphabet(normalizedInput: String): TransliterationResult =
                    TransliterationResult.Failure(
                        normalizedInput.mapIndexed { idx, c -> UnmappedCharacter(c, idx) },
                    )
            }
        val outcome = profile.transliterate("XY")
        val result = assertIs<TransliterationResult.Failure>(outcome.result)
        assertEquals(2, result.unmappedCharacters.size)
        assertEquals(UnmappedCharacter('X', 0), result.unmappedCharacters[0])
        assertEquals(UnmappedCharacter('Y', 1), result.unmappedCharacters[1])
        // Original and normalized forms are still carried even on Failure.
        assertEquals("XY", outcome.originalInput)
        assertEquals("XY", outcome.normalizedInput)
        assertEquals("TEST-FAIL-ONLY", outcome.profileIdentifier)
    }

    @Test
    fun aze_profile_normalization_and_transliteration_pipeline() {
        // Lowercase ə (U+0259) — NFC and NFD are the same single code point for schwa.
        val outcome = AzeTransliterationProfile.transliterate("ə")
        assertEquals("AZE", outcome.profileIdentifier)
        assertEquals("ə", outcome.normalizedInput)
        val result = assertIs<TransliterationResult.Success>(outcome.result)
        assertEquals("A", result.output)
    }
}
