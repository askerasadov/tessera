package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.domain.vocabulary.UnmappedCharacter

public sealed class TransliterationResult {
    public data class Success(
        val output: String,
    ) : TransliterationResult()

    public data class Failure(
        val unmappedCharacters: List<UnmappedCharacter>,
    ) : TransliterationResult()
}
