package io.lightine.tessera.mrz.validation

import io.lightine.tessera.domain.errors.MrzValidationError
import io.lightine.tessera.domain.errors.MrzWarning

public data class ValidationResult(
    val validationFailures: List<MrzValidationError>,
    val warnings: List<MrzWarning>,
)
