package io.lightine.tessera.mrz

public data class MrzCheckDigits(
    val documentNumber: Char,
    val dateOfBirth: Char,
    val dateOfExpiry: Char,
    val optionalData: Char?,
    val composite: Char,
)
