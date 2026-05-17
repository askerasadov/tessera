package io.lightine.tessera.domain.errors

/**
 * Sealed sub-root for errors emitted by
 * [`MrzParser`][io.lightine.tessera.mrz.parsing.MrzParser]. A parse error means the SDK
 * could not produce an [`MrzDocument`][io.lightine.tessera.mrz.model.MrzDocument] from
 * the input; the parser returns
 * [`ParseResult.Failure`][io.lightine.tessera.mrz.parsing.ParseResult.Failure] carrying
 * the concrete error and the raw input that produced it.
 *
 * See [MrzError] for the relationship to validation errors and warnings, which use
 * separate roots.
 */
public sealed class MrzParseError : MrzError()
