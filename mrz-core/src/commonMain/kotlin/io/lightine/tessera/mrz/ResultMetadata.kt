package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzValidationError
import io.lightine.tessera.domain.MrzWarning
import io.lightine.tessera.domain.ReadMethod

public data class ResultMetadata(
    val readMethod: ReadMethod,
    val warnings: List<MrzWarning>,
    val validationFailures: List<MrzValidationError>,
)
