package io.lightine.tessera.mrz.parsing

import io.lightine.tessera.domain.errors.MrzValidationError
import io.lightine.tessera.domain.errors.MrzWarning
import io.lightine.tessera.domain.vocabulary.ReadMethod

public data class ResultMetadata(
    val readMethod: ReadMethod,
    val warnings: List<MrzWarning>,
    val validationFailures: List<MrzValidationError>,
)
