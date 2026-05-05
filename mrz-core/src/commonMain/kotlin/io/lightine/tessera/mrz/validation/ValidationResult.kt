package io.lightine.tessera.mrz.validation

import io.lightine.tessera.domain.MrzValidationError
import io.lightine.tessera.domain.MrzWarning

public data class ValidationResult(
    val validationFailures: List<MrzValidationError>,
    val warnings: List<MrzWarning>,
)
