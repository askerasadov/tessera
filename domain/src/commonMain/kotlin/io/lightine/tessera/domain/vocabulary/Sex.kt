package io.lightine.tessera.domain.vocabulary

/**
 * The sex value recorded on the document. Decoded by the parser from the MRZ sex character
 * and used by the generator to choose the character to encode.
 *
 * MRZ encoding:
 * - [MALE] is encoded as `M`
 * - [FEMALE] is encoded as `F`
 * - [UNSPECIFIED] is encoded as the filler character `<`. ICAO Doc 9303 also permits `X` for
 *   unspecified; the generator currently emits `<` as its default. Consumers needing `X` in
 *   generator output is a deferred enhancement.
 *
 * Documents whose sex field contains a character outside `M`, `F`, `<`, `X` surface
 * [`MrzInvalidSexValue`][io.lightine.tessera.domain.errors.MrzInvalidSexValue] as a
 * validation failure rather than being silently coerced (Principle 1 — reader, not oracle).
 */
public enum class Sex {
    MALE,
    FEMALE,
    UNSPECIFIED,
}
