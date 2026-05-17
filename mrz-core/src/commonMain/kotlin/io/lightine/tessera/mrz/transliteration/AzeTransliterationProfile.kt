package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.mrz.parsing.isMrzAlphabetCharacter

// Transliteration profile for the issuing state with ISO 3166-1 alpha-3 code
// "AZE". The mapping mostly aligns with ICAO Doc 9303 Part 3 Section 6 (Annex G)
// under the no-expansion convention; the load-bearing divergence is the Latin
// schwa character — Ə/ə map to A here, where the ICAO default maps them to E.
// This matches observed practice in documents issued by this state and is the
// example called out in ADR-009 as the canonical reason profile divergence
// exists in the SDK at all.
//
// Unmapped non-MRZ characters fall back to the filler `<`, so this profile
// always returns Success.
//
// IMPORTANT: This is a deliberate starter set, not an exhaustive enumeration.
// Tracked in docs/open-questions.md under "Country-specific profile coverage
// completeness". Adding entries is a non-breaking change.
public object AzeTransliterationProfile : TransliterationProfile {
    public const val IDENTIFIER: String = "AZE"

    public override val identifier: String = IDENTIFIER

    private const val FILLER: Char = '<'

    private val mappings: Map<Char, String> =
        buildIcaoLatinMappings().apply {
            put('Ə', "A")
            put('ə', "A")
        }

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
