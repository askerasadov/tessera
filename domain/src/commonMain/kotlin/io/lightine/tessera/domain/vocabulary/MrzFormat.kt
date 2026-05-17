package io.lightine.tessera.domain.vocabulary

/**
 * The five MRZ formats defined by ICAO Doc 9303. Each
 * [`MrzDocument`][io.lightine.tessera.mrz.model.MrzDocument] variant reports its
 * format via this enum.
 *
 * - [TD1] — 3 lines × 30 characters, per ICAO Doc 9303 Part 5 (identity cards)
 * - [TD2] — 2 lines × 36 characters, per ICAO Doc 9303 Part 6 (smaller identity documents)
 * - [TD3] — 2 lines × 44 characters, per ICAO Doc 9303 Part 4 (passports)
 * - [MRV_A] — 2 lines × 44 characters, per ICAO Doc 9303 Part 7 (Type-A visas)
 * - [MRV_B] — 2 lines × 36 characters, per ICAO Doc 9303 Part 7 (Type-B visas)
 *
 * TD2 / MRV-B and TD3 / MRV-A share line dimensions; the parser disambiguates by the leading
 * `V` character that marks a visa. Format-specific shapes (composite check digit presence,
 * field positions, format-specific extras) are documented on the corresponding
 * [`MrzDocument`][io.lightine.tessera.mrz.model.MrzDocument] subtype.
 */
public enum class MrzFormat {
    TD1,
    TD2,
    TD3,
    MRV_A,
    MRV_B,
}
