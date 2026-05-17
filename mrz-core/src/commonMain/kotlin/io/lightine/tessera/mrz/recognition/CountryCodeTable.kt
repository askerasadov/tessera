package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.domain.vocabulary.CountryCodeCategory

/**
 * The SDK's recognized country codes for MRZ issuing state and nationality positions.
 *
 * **Deliberate starter set.** This table is intentionally incomplete relative to the
 * canonical sources (ISO 3166-1 alpha-3 and ICAO Doc 9303 Part 3 Section 5, which adds
 * codes for organizations, stateless persons, refugees, and historical states). Adding
 * entries is a non-breaking change. See
 * [`docs/features/lookup-tables.md`](https://github.com/askerasadov/Tessera/blob/main/docs/features/lookup-tables.md)
 * for the design and `docs/open-questions.md` for tracking ("Country code table
 * completeness").
 *
 * Codes not present in this table surface as
 * [`MrzUnknownCountryCode`][io.lightine.tessera.domain.errors.MrzUnknownCountryCode]
 * warnings rather than validation failures, per
 * [ADR-013](https://github.com/askerasadov/Tessera/blob/main/docs/decisions/0013-recognition-failures-are-warnings.md).
 */
public object CountryCodeTable {
    private val entries: List<CountryCodeEntry> =
        listOf(
            CountryCodeEntry(code = "USA", displayName = "United States of America", category = CountryCodeCategory.STATE),
            CountryCodeEntry(code = "GBR", displayName = "United Kingdom", category = CountryCodeCategory.STATE),
            CountryCodeEntry(code = "DEU", displayName = "Germany", category = CountryCodeCategory.STATE),
            CountryCodeEntry(code = "FRA", displayName = "France", category = CountryCodeCategory.STATE),
            CountryCodeEntry(code = "JPN", displayName = "Japan", category = CountryCodeCategory.STATE),
        )

    private val byCode: Map<String, CountryCodeEntry> = entries.associateBy { it.code }

    /** Returns the entry for [code], or `null` if the code is not in the table. */
    public fun lookup(code: String): CountryCodeEntry? = byCode[code]

    /** Returns every entry currently in the table, in registration order. */
    public fun all(): List<CountryCodeEntry> = entries

    /** Returns every entry in the table whose category matches [category]. */
    public fun byCategory(category: CountryCodeCategory): List<CountryCodeEntry> = entries.filter { it.category == category }
}
