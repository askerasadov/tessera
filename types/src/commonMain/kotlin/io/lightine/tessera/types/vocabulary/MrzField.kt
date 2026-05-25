package io.lightine.tessera.types.vocabulary

/**
 * A symbolic name for an MRZ field, used as a discriminator on errors and warnings that
 * need to identify which field they refer to.
 *
 * Used by error types such as
 * [`MrzCheckDigitMismatch`][io.lightine.tessera.types.errors.MrzCheckDigitMismatch],
 * [`MrzDateNotInCalendar`][io.lightine.tessera.types.errors.MrzDateNotInCalendar],
 * [`MrzGenerationFieldOverflow`][io.lightine.tessera.types.errors.MrzGenerationFieldOverflow],
 * and others. Position information is carried alongside the field name so consumers can
 * render precise per-character diagnostics.
 *
 * The enumerated names are SDK-internal symbols, not strings drawn from any external
 * specification.
 */
public enum class MrzField {
    DOCUMENT_TYPE,
    ISSUING_STATE,
    NAME_FIELD,
    DOCUMENT_NUMBER,
    NATIONALITY,
    DATE_OF_BIRTH,
    DATE_OF_EXPIRY,
    OPTIONAL_DATA,
    COMPOSITE,
}
