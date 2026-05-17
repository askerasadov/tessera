package io.lightine.tessera.domain.errors

/**
 * Root of the SDK's hard-error taxonomy: situations that prevent an operation from
 * producing a meaningful result. Returned through `Failure` variants on result types —
 * [`ParseResult.Failure`][io.lightine.tessera.mrz.parsing.ParseResult.Failure] for the
 * parser and [`GenerationResult.Failure`][io.lightine.tessera.mrz.generation.GenerationResult.Failure]
 * for the generator.
 *
 * Two intermediate sealed sub-roots categorize errors by subsystem:
 * - [MrzParseError] — emitted by the parser; the SDK could not produce a document
 * - [MrzGenerationError] — emitted by the generator; the SDK could not produce an MRZ
 *
 * Soft observations that do not prevent an operation from succeeding live under separate
 * roots: [MrzValidationError] (per-field validation failures, populated on result
 * metadata even when the operation otherwise succeeds) and [MrzWarning] (informational
 * observations such as truncation or implausible dates). The three roots are deliberately
 * not unified so consumers cannot accidentally conflate the categories.
 *
 * Every error carries a [description] suitable for surfacing in logs or UI; the structured
 * fields on each concrete subtype carry the same information in a machine-readable form.
 */
public sealed class MrzError {
    public abstract val description: String
}
