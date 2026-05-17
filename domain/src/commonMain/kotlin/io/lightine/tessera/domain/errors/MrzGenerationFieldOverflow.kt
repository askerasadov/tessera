package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat

public data class MrzGenerationFieldOverflow(
    val format: MrzFormat,
    val field: MrzField,
    val maxLength: Int,
    val observedLength: Int,
    val observedValue: String,
) : MrzGenerationError() {
    override val description: String
        get() =
            "Format $format field ${this.field} expected at most $maxLength characters; " +
                "observed $observedLength characters: \"$observedValue\""
}
