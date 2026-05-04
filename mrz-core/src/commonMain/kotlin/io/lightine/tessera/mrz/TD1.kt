package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzFormat

public data class TD1(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val optionalData1: String,
    val optionalData2: String,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.TD1
}
