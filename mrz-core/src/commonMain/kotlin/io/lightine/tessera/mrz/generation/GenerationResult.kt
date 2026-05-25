package io.lightine.tessera.mrz.generation

import io.lightine.tessera.mrz.parsing.ResultMetadata
import io.lightine.tessera.types.errors.MrzGenerationError

/**
 * The outcome of [`MrzGenerator`][MrzGenerator] encoding a document into MRZ lines. Two
 * variants:
 *
 * - [Success] — the encoded [`mrz`][Success.mrz] lines are ready to be assembled (joined
 *   with newlines if a single string is needed).
 * - [Failure] — generation could not produce a valid MRZ. Carries the
 *   [`error`][Failure.error] explaining why.
 *
 * Every variant carries
 * [`ResultMetadata`][io.lightine.tessera.mrz.parsing.ResultMetadata]; for the
 * primitive-input methods that accept a transliteration profile, the metadata's
 * `transliterationDetails` field carries the per-field audit trail of the original input,
 * normalized form, and transliterated output.
 *
 * Unlike [`ParseResult`][io.lightine.tessera.mrz.parsing.ParseResult] there is no
 * `PartialSuccess` — generation either produces a valid MRZ or it does not.
 */
public sealed class GenerationResult {
    public abstract val metadata: ResultMetadata

    public data class Success(
        val mrz: List<String>,
        override val metadata: ResultMetadata,
    ) : GenerationResult()

    public data class Failure(
        val error: MrzGenerationError,
        override val metadata: ResultMetadata,
    ) : GenerationResult()
}
