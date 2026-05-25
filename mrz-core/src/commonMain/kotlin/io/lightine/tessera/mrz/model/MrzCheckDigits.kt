package io.lightine.tessera.mrz.model

/**
 * The check digits exactly as recorded on the document.
 *
 * These are the values the SDK read from the MRZ, not the values the SDK would compute
 * from the field content (Principle 5 — transparency).
 * [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator] cross-checks each
 * recorded digit against the computed value and emits
 * [`MrzCheckDigitMismatch`][io.lightine.tessera.types.errors.MrzCheckDigitMismatch] on
 * mismatch.
 *
 * [optionalData] is nullable because not every format has an optional-data check digit
 * (TD2 doesn't); [composite] is nullable because the visa formats (MRV-A, MRV-B) per
 * ICAO Doc 9303 Part 7 do not have a composite check digit.
 */
public data class MrzCheckDigits(
    val documentNumber: Char,
    val dateOfBirth: Char,
    val dateOfExpiry: Char,
    val optionalData: Char?,
    val composite: Char?,
)
