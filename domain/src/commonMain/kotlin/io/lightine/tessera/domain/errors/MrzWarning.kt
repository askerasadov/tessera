package io.lightine.tessera.domain.errors

/**
 * Root of the SDK's warning taxonomy: informational observations that do not prevent the
 * document from being usable, but that consumers may want to surface or act on.
 *
 * Warnings are emitted by [`MrzParser`][io.lightine.tessera.mrz.parsing.MrzParser] and
 * [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator] and populated in
 * [`ResultMetadata.warnings`][io.lightine.tessera.mrz.parsing.ResultMetadata]. A result
 * with warnings but no validation failures is still
 * [`ParseResult.Success`][io.lightine.tessera.mrz.parsing.ParseResult.Success] — warnings
 * are informational, not disqualifying. Consumers who want strict behavior can check
 * `warnings.isEmpty()` together with `validationFailures.isEmpty()`.
 *
 * Examples include implausibly-old birth dates ([MrzBirthDateImplausiblyOld]), expiry dates
 * far in the future ([MrzExpiryDateImplausiblyFar]), name-field truncation
 * ([MrzNameTruncated]), and country/document codes the SDK does not yet recognize
 * ([MrzUnknownCountryCode], [MrzUnknownDocumentTypeCode]).
 *
 * The root is deliberately separate from [MrzError] and [MrzValidationError] so consumers
 * cannot accidentally conflate informational observations with hard or per-field failures.
 *
 * Every warning carries a [description] suitable for surfacing in logs or UI; the
 * structured fields on each concrete subtype carry the same information in a
 * machine-readable form.
 */
public sealed class MrzWarning {
    public abstract val description: String
}
