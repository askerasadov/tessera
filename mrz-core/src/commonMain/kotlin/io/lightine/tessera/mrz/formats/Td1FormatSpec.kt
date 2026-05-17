package io.lightine.tessera.mrz.formats

public object Td1FormatSpec {
    public val lineCount: Int = 3
    public val lineLength: Int = 30

    // Line 1 (30 chars): doc type, issuing state, document number + check, optional data 1
    public val documentType: FieldSpec = FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2)
    public val issuingState: FieldSpec = FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5)
    public val documentNumber: FieldSpec = FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 14)
    public val documentNumberCheckDigit: FieldSpec = FieldSpec(line = 0, startInLine = 14, endInLineExclusive = 15)
    public val optionalData1: FieldSpec = FieldSpec(line = 0, startInLine = 15, endInLineExclusive = 30)

    // Line 2 (30 chars): DOB + check, sex, DOE + check, nationality, optional data 2, composite
    public val dateOfBirth: FieldSpec = FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 6)
    public val dateOfBirthCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 6, endInLineExclusive = 7)
    public val sex: FieldSpec = FieldSpec(line = 1, startInLine = 7, endInLineExclusive = 8)
    public val dateOfExpiry: FieldSpec = FieldSpec(line = 1, startInLine = 8, endInLineExclusive = 14)
    public val dateOfExpiryCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 14, endInLineExclusive = 15)
    public val nationality: FieldSpec = FieldSpec(line = 1, startInLine = 15, endInLineExclusive = 18)
    public val optionalData2: FieldSpec = FieldSpec(line = 1, startInLine = 18, endInLineExclusive = 29)
    public val compositeCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 29, endInLineExclusive = 30)

    // Line 3 (30 chars): name field
    public val rawNameField: FieldSpec = FieldSpec(line = 2, startInLine = 0, endInLineExclusive = 30)

    // Per ICAO Doc 9303 Part 5: composite check digit covers line 1 positions 6-30 (doc number
    // + its check digit + optional data 1) and line 2 positions 1-7 (DOB + its check digit),
    // 9-15 (DOE + its check digit), 19-29 (optional data 2). Sex (line 2 position 8), nationality
    // (line 2 positions 16-18), and the composite digit itself (line 2 position 30) are excluded.
    public val compositeInputFields: List<FieldSpec> =
        listOf(
            FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 30),
            FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 7),
            FieldSpec(line = 1, startInLine = 8, endInLineExclusive = 15),
            FieldSpec(line = 1, startInLine = 18, endInLineExclusive = 29),
        )

    public fun globalPositionOf(field: FieldSpec): Int = field.line * lineLength + field.startInLine
}
