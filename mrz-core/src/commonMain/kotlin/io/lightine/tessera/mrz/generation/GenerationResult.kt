package io.lightine.tessera.mrz.generation

import io.lightine.tessera.domain.errors.MrzGenerationError
import io.lightine.tessera.mrz.parsing.ResultMetadata

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
