package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzFormat

public data class TD3(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val personalNumber: String,
    val personalNumberCheckDigit: Char,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.TD3
}
