package io.lightine.tessera.types.vocabulary

/**
 * A single character that a transliteration profile could not map into the MRZ alphabet,
 * together with its zero-based position in the post-normalization input string.
 *
 * Carried in lists by
 * [`TransliterationResult.Failure`][io.lightine.tessera.mrz.transliteration.TransliterationResult.Failure]
 * and by
 * [`MrzGenerationUnsupportedCharacters`][io.lightine.tessera.types.errors.MrzGenerationUnsupportedCharacters]
 * so consumers can identify exactly which characters were problematic and where they
 * appeared. Lives in `types.vocabulary` rather than the transliteration package so that
 * error types in the `types` module can reference it without depending on `mrz-core`.
 */
public data class UnmappedCharacter(
    val character: Char,
    val position: Int,
)
