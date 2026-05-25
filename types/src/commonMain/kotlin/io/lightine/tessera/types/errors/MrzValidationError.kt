package io.lightine.tessera.types.errors

/**
 * Root of the SDK's per-field validation taxonomy: structural checks that the SDK could
 * not pass for one or more individual fields, even though the input parsed successfully
 * at the format-and-length level.
 *
 * Validation errors are emitted by
 * [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator] and populated in
 * [`ResultMetadata.validationFailures`][io.lightine.tessera.mrz.parsing.ResultMetadata].
 * Their presence causes the parser to return
 * [`ParseResult.PartialSuccess`][io.lightine.tessera.mrz.parsing.ParseResult.PartialSuccess]
 * (the document is structurally valid but failed one or more per-field checks) rather
 * than the hard [`ParseResult.Failure`][io.lightine.tessera.mrz.parsing.ParseResult.Failure]
 * reserved for [MrzParseError]s.
 *
 * The root is deliberately separate from [MrzError] so consumers cannot accidentally
 * conflate "I got a document with some bad fields" with "parsing failed entirely."
 *
 * Every validation error carries a [description] suitable for surfacing in logs or UI;
 * the structured fields on each concrete subtype carry the same information in a
 * machine-readable form.
 */
public sealed class MrzValidationError {
    public abstract val description: String
}
