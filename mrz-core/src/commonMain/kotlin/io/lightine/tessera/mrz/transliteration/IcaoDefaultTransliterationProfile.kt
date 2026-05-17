package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.mrz.parsing.isMrzAlphabetCharacter

/**
 * The default transliteration profile. Implements the recommendations in ICAO Doc 9303
 * Part 3 Section 6 (Annex G) under the no-expansion convention:
 *
 * - Single-character substitutions: `Ä → A`, `ü → U`, `é → E`, etc. — accented Latin
 *   letters collapse to their unaccented base.
 * - Multi-character substitutions per the standard: `Æ → AE`, `Œ → OE`, `ß → SS`,
 *   `Þ → TH`, `Ð → D`, `Ĳ → IJ`. The Latin schwa `Ə`/`ə` maps to `E`.
 *
 * Unmapped non-MRZ characters fall back to the filler `<`, so this profile always returns
 * [`TransliterationResult.Success`][TransliterationResult.Success] — it never reports
 * unmapped characters.
 *
 * **Deliberate starter set.** This profile covers the ICAO Annex G recommendations and
 * the common European Latin script. It does not yet cover non-Latin scripts (Cyrillic,
 * Greek, Arabic, etc.) or every Latin variant in use. Adding entries is a non-breaking
 * change; tracked in `docs/open-questions.md` under "ICAO default profile coverage
 * completeness".
 *
 * Country-specific divergences from this profile are captured by separate profiles such
 * as [AzeTransliterationProfile].
 */
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
