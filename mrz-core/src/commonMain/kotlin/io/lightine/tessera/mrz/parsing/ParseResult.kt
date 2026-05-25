package io.lightine.tessera.mrz.parsing

import io.lightine.tessera.mrz.model.MrzDocument
import io.lightine.tessera.types.errors.MrzParseError

/**
 * The outcome of [`MrzParser`][MrzParser.parse] parsing input. Three variants:
 *
 * - [Success] — the input parsed cleanly and the validator surfaced no failures (warnings
 *   may still be present in [metadata]).
 * - [PartialSuccess] — the input parsed cleanly at the structural level but the validator
 *   surfaced one or more per-field failures. The [`document`][PartialSuccess.document] is
 *   still populated; consumers decide whether to trust it given the validation failures
 *   in [metadata].
 * - [Failure] — the input could not be parsed at all (wrong shape, characters outside the
 *   MRZ alphabet, no recognized format). Carries the [`error`][Failure.error] and the
 *   raw input that produced it.
 *
 * Every variant carries [metadata]: [`ReadMethod`][io.lightine.tessera.types.vocabulary.ReadMethod],
 * any [`MrzWarning`][io.lightine.tessera.types.errors.MrzWarning]s, any
 * [`MrzValidationError`][io.lightine.tessera.types.errors.MrzValidationError]s, and the
 * transliteration audit trail (which is `null` for parser-produced results since the
 * parser does not transliterate).
 */
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
