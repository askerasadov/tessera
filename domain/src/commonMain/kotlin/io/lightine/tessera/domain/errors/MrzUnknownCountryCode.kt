package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField

public data class MrzUnknownCountryCode(
    val field: MrzField,
    val rawCode: String,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() = "Country code '$rawCode' for ${this.field} at position $position is not in the SDK's recognized lookup tables"
}
