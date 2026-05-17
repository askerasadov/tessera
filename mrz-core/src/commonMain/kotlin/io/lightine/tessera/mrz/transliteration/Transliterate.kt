package io.lightine.tessera.mrz.transliteration

// Public end-to-end transliteration entry point. Composes Unicode normalization
// (per ADR-014, internal) with profile application (per ADR-009), surfacing
// the post-normalization, pre-transliteration form alongside the original input
// and the profile's output per Principle 5.
public fun TransliterationProfile.transliterate(input: String): TransliterationOutcome {
    val normalized = normalizeForTransliteration(input)
    val result = this.toMrzAlphabet(normalized)
    return TransliterationOutcome(
        profileIdentifier = this.identifier,
        originalInput = input,
        normalizedInput = normalized,
        result = result,
    )
}

public data class TransliterationOutcome(
    val profileIdentifier: String,
    val originalInput: String,
    val normalizedInput: String,
    val result: TransliterationResult,
)
