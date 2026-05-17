package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.domain.vocabulary.CountryCodeCategory

public data class CountryCodeEntry(
    val code: String,
    val displayName: String,
    val category: CountryCodeCategory,
)
