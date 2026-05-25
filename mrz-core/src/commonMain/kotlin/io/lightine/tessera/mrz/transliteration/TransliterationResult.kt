package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.types.vocabulary.UnmappedCharacter

/**
 * The outcome of a [TransliterationProfile] applying its mapping rules to a normalized
 * input string. Sealed root with two variants:
 *
 * - [Success] — every input character was either already in the MRZ alphabet or mapped
 *   to one or more MRZ-alphabet characters by the profile. Profiles with a documented
 *   fallback policy (such as the shipped ICAO and AZE profiles, which map unrecognized
 *   characters to the filler `<`) always return [Success].
 * - [Failure] — at least one input character could not be mapped, and the profile has
 *   no fallback policy. The unmappable characters are returned via [Failure.unmappedCharacters]
 *   so consumers can identify exactly what failed and decide how to recover.
 *
 * Matches the project's sealed-result-type pattern (
 * [`ParseResult`][io.lightine.tessera.mrz.parsing.ParseResult],
 * [`GenerationResult`][io.lightine.tessera.mrz.generation.GenerationResult]).
 */
public sealed class TransliterationResult {
    /** Every input character was handled; [output] is in the MRZ alphabet. */
    public data class Success(
        val output: String,
    ) : TransliterationResult()

    /** One or more input characters could not be mapped and no fallback applied. */
    public data class Failure(
        val unmappedCharacters: List<UnmappedCharacter>,
    ) : TransliterationResult()
}
