package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.mrz.parsing.isMrzAlphabetCharacter

// Default transliteration profile, implementing the recommendations in
// ICAO Doc 9303 Part 3 Section 6 (Annex G) under the no-expansion convention
// (e.g., Ä → A, ü → U). Multi-character transliterations follow the standard:
// Æ → AE, Œ → OE, ß → SS, Þ → TH, Ð → D, Ĳ → IJ. Schwa Ə/ə → E per ICAO.
//
// Unmapped non-MRZ characters fall back to the filler `<`, so this profile
// always returns Success.
//
// IMPORTANT: This is a deliberate starter set, not an exhaustive enumeration.
// Tracked in docs/open-questions.md under "ICAO default profile coverage
// completeness". Adding entries is a non-breaking change.
public object IcaoDefaultTransliterationProfile : TransliterationProfile {
    public const val IDENTIFIER: String = "ICAO"

    public override val identifier: String = IDENTIFIER

    private const val FILLER: Char = '<'

    private val mappings: Map<Char, String> = buildIcaoLatinMappings()

    public override fun toMrzAlphabet(normalizedInput: String): TransliterationResult {
        val output = StringBuilder(normalizedInput.length)
        for (char in normalizedInput) {
            when {
                isMrzAlphabetCharacter(char) -> output.append(char)
                else -> output.append(mappings[char] ?: FILLER.toString())
            }
        }
        return TransliterationResult.Success(output.toString())
    }
}
