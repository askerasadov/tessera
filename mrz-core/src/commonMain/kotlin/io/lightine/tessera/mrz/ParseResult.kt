package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzParseError

public sealed class ParseResult {
    public abstract val metadata: ResultMetadata

    public data class Success(
        val document: MrzDocument,
        override val metadata: ResultMetadata,
    ) : ParseResult()

    public data class PartialSuccess(
        val document: MrzDocument,
        override val metadata: ResultMetadata,
    ) : ParseResult()

    public data class Failure(
        val error: MrzParseError,
        val rawInput: String?,
        override val metadata: ResultMetadata,
    ) : ParseResult()
}
