package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat

public data class MrzGenerationMissingRequiredField(
    val format: MrzFormat,
    val field: MrzField,
) : MrzGenerationError() {
    override val description: String
        get() = "Format $format requires field ${this.field}; the provided input did not supply a value"
}
