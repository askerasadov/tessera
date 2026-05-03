package io.lightine.tessera.mrz

import io.lightine.tessera.domain.Sex

public data class CommonFields(
    val documentType: String,
    val issuingState: String,
    val primaryIdentifier: String,
    val secondaryIdentifier: String,
    val nameTruncated: Boolean,
    val rawNameField: String,
    val documentNumber: String,
    val nationality: String,
    val dateOfBirth: MrzDate,
    val sex: Sex,
    val dateOfExpiry: MrzDate,
    val checkDigits: MrzCheckDigits,
)
