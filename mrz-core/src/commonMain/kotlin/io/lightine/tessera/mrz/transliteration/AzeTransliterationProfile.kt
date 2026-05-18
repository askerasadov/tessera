package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.mrz.parsing.isMrzAlphabetCharacter

/**
 * Transliteration profile for the issuing state with ISO 3166-1 alpha-3 code `"AZE"`.
 *
 * The mapping inherits the ICAO Doc 9303 Part 3 §6.A Latin recommendations (no-expansion
 * convention) for every covered character. The load-bearing override is the Latin schwa
 * `Ə`/`ə` (U+018F / U+0259), which maps to `A`.
 *
 * **Why the schwa maps to `A`.** Two standards-grounded reasons converge on the same
 * answer:
 *
 * 1. **BGN/PCGN 1993 Agreement** (UK government romanization system for the AZE
 *    alphabet, 2022 revision) Note 1: *"The special letter Ə, ə known as schwa, should be reproduced
 *    in that form whenever encountered. In those instances when it cannot be reproduced,
 *    however, the letter Ä ä may be substituted for it."* In the MRZ alphabet, schwa
 *    cannot be reproduced.
 * 2. **ICAO Doc 9303 Part 3 Annex G** maps `Ä → A` under the no-expansion convention.
 *
 * Chained: `Ə (schwa) → Ä (BGN/PCGN fallback) → A (ICAO Annex G no-expansion)`. This
 * matches observed practice in passports issued under code AZE. Schwa is not in ICAO
 * Annex G's table (which ends at U+017D plus U+1E9E, outside the Latin Extended-B range
 * where schwa lives), so the override is necessary regardless.
 *
 * For other letters in the AZE Latin alphabet (`Ç`, `Ğ`, `İ`, `ı`, `Ş` — single mapping
 * in Annex G; `Ö`, `Ü` — Annex G permits both no-expansion and expanded variants, this
 * profile uses no-expansion for consistency), the ICAO defaults from [`buildIcaoLatinMappings`]
 * are inherited without further override.
 *
 * Unmapped non-MRZ characters fall back to the filler `<`, so this profile always returns
 * [`TransliterationResult.Success`][TransliterationResult.Success].
 *
 * **Deliberate starter set.** The `Ö`/`Ü` no-expansion choice is empirically unverified
 * against a sample of AZE-issued passports; if observed practice for this issuing state
 * diverges (e.g., `Ö → OE` instead of `Ö → O`), additional overrides ship in a future
 * release. Tracked in `docs/open-questions.md` under "AZE profile `Ö`/`Ü` empirical
 * verification" and "Country-specific profile coverage completeness". Per the project's
 * vendor-neutral framing, the country is identified by its ISO code in code and never
 * named in prose.
 */
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
