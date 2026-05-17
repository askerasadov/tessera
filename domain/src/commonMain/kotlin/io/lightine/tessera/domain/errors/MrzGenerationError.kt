package io.lightine.tessera.domain.errors

/**
 * Sealed sub-root for errors emitted by
 * [`MrzGenerator`][io.lightine.tessera.mrz.generation.MrzGenerator]. A generation error
 * means the SDK could not encode the supplied input into a valid MRZ string; the generator
 * returns
 * [`GenerationResult.Failure`][io.lightine.tessera.mrz.generation.GenerationResult.Failure]
 * carrying the concrete error.
 *
 * See [MrzError] for the relationship to validation errors and warnings, which use
 * separate roots.
 */
public sealed class MrzGenerationError : MrzError()
