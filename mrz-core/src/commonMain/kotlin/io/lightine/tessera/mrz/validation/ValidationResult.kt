package io.lightine.tessera.mrz.validation

import io.lightine.tessera.domain.errors.MrzValidationError
import io.lightine.tessera.domain.errors.MrzWarning

/**
 * The output of [`MrzValidator.validate`][MrzValidator.validate]: per-field validation
 * failures and informational warnings emitted while checking a parsed
 * [`MrzDocument`][io.lightine.tessera.mrz.model.MrzDocument].
 *
 * Used directly by callers of the validator. The parser also invokes the validator
 * internally and folds the result into
 * [`ResultMetadata`][io.lightine.tessera.mrz.parsing.ResultMetadata] so consumers of the
 * parse path receive validation results alongside the parsed document.
 *
 * A `ValidationResult` with empty [validationFailures] and empty [warnings] indicates the
 * document passed every check the validator currently applies.
 */
public data class ValidationResult(
    val validationFailures: List<MrzValidationError>,
    val warnings: List<MrzWarning>,
)
