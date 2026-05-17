package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.domain.vocabulary.CountryCodeCategory

/**
 * A single entry in [CountryCodeTable]: the raw three-character code as it appears in an
 * MRZ, the SDK's display name for it, and its category (state, organization, stateless,
 * refugee, historical, other).
 *
 * Returned by [CountryCodeTable.lookup] and indirectly via [CountryCode.entry].
 */
public data class CountryCodeEntry(
    val code: String,
    val displayName: String,
    val category: CountryCodeCategory,
)
