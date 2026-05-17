package io.lightine.tessera.mrz.formats

/**
 * Field positions for the TD2 identity document format per ICAO Doc 9303 Part 6. Two
 * lines × 36 characters; same line dimensions as MRV-B, disambiguated by the leading
 * character (TD2 does not start with `V`).
 *
 * Adds [optionalData] beyond the base [MrzFormatSpec] surface; the composite check digit
 * covers the document number + check, date of birth + check, and date of expiry + check
 * + optional data (sex and the composite digit itself are excluded).
 */
public object Td2FormatSpec : MrzFormatSpecWithComposite {
    override val lineCount: Int = 2
    override val lineLength: Int = 36

    override val documentType: FieldSpec = FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2)
    override val issuingState: FieldSpec = FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5)
    override val rawNameField: FieldSpec = FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 36)

    override val documentNumber: FieldSpec = FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 9)
    override val documentNumberCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 9, endInLineExclusive = 10)
    override val nationality: FieldSpec = FieldSpec(line = 1, startInLine = 10, endInLineExclusive = 13)
    override val dateOfBirth: FieldSpec = FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19)
    override val dateOfBirthCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 19, endInLineExclusive = 20)
    override val sex: FieldSpec = FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21)
    override val dateOfExpiry: FieldSpec = FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 27)
    override val dateOfExpiryCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 27, endInLineExclusive = 28)
    public val optionalData: FieldSpec = FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 35)
    override val compositeCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 35, endInLineExclusive = 36)

    // Per ICAO Doc 9303 Part 6: composite check digit covers document number + its check digit,
    // date of birth + its check digit, and date of expiry + its check digit + optional data
    // (optional data has no per-field check digit in TD2). Sex and the composite digit itself
    // are excluded.
    override val compositeInputFields: List<FieldSpec> =
        listOf(
            FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 10),
            FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 20),
            FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 35),
        )
}
