package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat

/**
 * Generation error: the input passed to
 * [`MrzGenerator`][io.lightine.tessera.mrz.generation.MrzGenerator] did not provide a
 * value for a [field] that the [format] requires.
 *
 * The set of required fields per format is defined by ICAO Doc 9303 and documented on
 * the relevant
 * [`MrzFormatSpec`][io.lightine.tessera.mrz.formats.MrzFormatSpec] subtype.
 */
public data class MrzGenerationMissingRequiredField(
    val format: MrzFormat,
    val field: MrzField,
) : MrzGenerationError() {
    override val description: String
        get() = "Format $format requires field ${this.field}; the provided input did not supply a value"
}
