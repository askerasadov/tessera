package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.MrzFormat

public data class TD2(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val optionalData: String,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.TD2
}
