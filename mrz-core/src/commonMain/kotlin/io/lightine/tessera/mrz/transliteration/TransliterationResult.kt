package io.lightine.tessera.mrz.transliteration

public sealed class TransliterationResult {
    public data class Success(
        val output: String,
    ) : TransliterationResult()

    public data class Failure(
        val unmappedCharacters: List<UnmappedCharacter>,
    ) : TransliterationResult()
}
