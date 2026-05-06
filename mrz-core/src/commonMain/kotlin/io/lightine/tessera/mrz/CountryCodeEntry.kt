package io.lightine.tessera.mrz

import io.lightine.tessera.domain.CountryCodeCategory

public data class CountryCodeEntry(
    val code: String,
    val displayName: String,
    val category: CountryCodeCategory,
)
