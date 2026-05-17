package io.lightine.tessera.mrz.model

public data class MrzCheckDigits(
    val documentNumber: Char,
    val dateOfBirth: Char,
    val dateOfExpiry: Char,
    val optionalData: Char?,
    val composite: Char,
)
