package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat

/**
 * Generation error: a field value supplied to
 * [`MrzGenerator`][io.lightine.tessera.mrz.generation.MrzGenerator] exceeds the maximum
 * length the [format] permits for that [field]. Carries both the [maxLength] and the
 * [observedLength] so consumers can decide how to abbreviate or truncate.
 *
 * The SDK does not auto-truncate (Principle 1 — reader, not oracle); the consumer must
 * decide what to do with the overflow.
 */
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
