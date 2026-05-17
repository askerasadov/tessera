package io.lightine.tessera.mrz.formats

public object Td2FormatSpec {
    public val lineCount: Int = 2
    public val lineLength: Int = 36

    public val documentType: FieldSpec = FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2)
    public val issuingState: FieldSpec = FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5)
    public val rawNameField: FieldSpec = FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 36)

    public val documentNumber: FieldSpec = FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 9)
    public val documentNumberCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 9, endInLineExclusive = 10)
    public val nationality: FieldSpec = FieldSpec(line = 1, startInLine = 10, endInLineExclusive = 13)
    public val dateOfBirth: FieldSpec = FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19)
    public val dateOfBirthCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 19, endInLineExclusive = 20)
    public val sex: FieldSpec = FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21)
    public val dateOfExpiry: FieldSpec = FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 27)
    public val dateOfExpiryCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 27, endInLineExclusive = 28)
    public val optionalData: FieldSpec = FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 35)
    public val compositeCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 35, endInLineExclusive = 36)

    // Per ICAO Doc 9303 Part 6: composite check digit covers document number + its check digit,
    // date of birth + its check digit, and date of expiry + its check digit + optional data
    // (optional data has no per-field check digit in TD2). Sex and the composite digit itself
    // are excluded.
    public val compositeInputFields: List<FieldSpec> =
        listOf(
            FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 10),
            FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 20),
            FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 35),
        )

    public fun globalPositionOf(field: FieldSpec): Int = field.line * lineLength + field.startInLine
}
