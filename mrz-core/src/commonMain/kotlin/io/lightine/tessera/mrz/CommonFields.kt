package io.lightine.tessera.mrz

import io.lightine.tessera.domain.Sex

public data class CommonFields(
    val documentType: DocumentType,
    val issuingState: CountryCode,
    val primaryIdentifier: String,
    val secondaryIdentifier: String,
    val nameTruncated: Boolean,
    val rawNameField: String,
    val documentNumber: String,
    val nationality: CountryCode,
    val dateOfBirth: MrzDate,
    val sex: Sex,
    val rawSex: Char,
    val dateOfExpiry: MrzDate,
    val checkDigits: MrzCheckDigits,
)
