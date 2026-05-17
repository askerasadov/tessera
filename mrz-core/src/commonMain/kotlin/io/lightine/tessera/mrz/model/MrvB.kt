package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.MrzFormat

public data class MrvB(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val optionalData: String,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.MRV_B
}
