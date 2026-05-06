package io.lightine.tessera.mrz

import io.lightine.tessera.domain.CountryCodeCategory

// IMPORTANT: This is a deliberate starter set, not the complete enumeration
// committed to in docs/features/lookup-tables.md. The canonical sources are
// ISO 3166-1 alpha-3 and ICAO Doc 9303 Part 3 Section 5 (which adds extensions
// for organizations, stateless persons, refugees, and historical states).
// Adding entries is a non-breaking change (see docs/features/lookup-tables.md).
// Tracked in docs/open-questions.md under "Country code table completeness".
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

    public fun lookup(code: String): CountryCodeEntry? = byCode[code]

    public fun all(): List<CountryCodeEntry> = entries

    public fun byCategory(category: CountryCodeCategory): List<CountryCodeEntry> = entries.filter { it.category == category }
}
