package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.MrzFormat

/**
 * A parsed MRZ document. Sealed root with one concrete subtype per supported MRZ format:
 * [TD1] (3 × 30 identity cards), [TD2] (2 × 36 smaller identity documents),
 * [TD3] (2 × 44 passports), [MrvA] (2 × 44 Type-A visas), and [MrvB] (2 × 36 Type-B visas).
 *
 * Every subtype carries the [rawLines] that produced it, an [`MrzFormat`][io.lightine.tessera.domain.vocabulary.MrzFormat]
 * tag in [format], and the field set shared across all formats in [commonFields].
 * Format-specific extras (e.g., TD3's personal number, TD1's two optional data fields) are
 * properties on the subtype.
 *
 * Produced by [`MrzParser`][io.lightine.tessera.mrz.parsing.MrzParser] inside
 * [`ParseResult.Success`][io.lightine.tessera.mrz.parsing.ParseResult.Success] or
 * [`ParseResult.PartialSuccess`][io.lightine.tessera.mrz.parsing.ParseResult.PartialSuccess];
 * consumed by [`MrzGenerator`][io.lightine.tessera.mrz.generation.MrzGenerator] when
 * encoding a document object back into an MRZ string.
 */
public sealed class MrzDocument {
    public abstract val rawLines: List<String>
    public abstract val format: MrzFormat
    public abstract val commonFields: CommonFields
}
