package io.lightine.tessera.mrz.formats

/**
 * Field positions for the TD3 passport format per ICAO Doc 9303 Part 4. Two lines × 44
 * characters; same line dimensions as MRV-A, disambiguated by the leading character (TD3
 * does not start with `V`).
 *
 * Adds [personalNumber] and [personalNumberCheckDigit] beyond the base [MrzFormatSpec]
 * surface; the composite check digit covers the document number + check, date of birth
 * + check, date of expiry + check, and personal number + check (sex and the composite
 * digit itself are excluded).
 */
public object Td3FormatSpec : MrzFormatSpecWithComposite {
    override val lineCount: Int = 2
    override val lineLength: Int = 44

    override val documentType: FieldSpec = FieldSpec(line = 0, startInLine = 0, endInLineExclusive = 2)
    override val issuingState: FieldSpec = FieldSpec(line = 0, startInLine = 2, endInLineExclusive = 5)
    override val rawNameField: FieldSpec = FieldSpec(line = 0, startInLine = 5, endInLineExclusive = 44)

    override val documentNumber: FieldSpec = FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 9)
    override val documentNumberCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 9, endInLineExclusive = 10)
    override val nationality: FieldSpec = FieldSpec(line = 1, startInLine = 10, endInLineExclusive = 13)
    override val dateOfBirth: FieldSpec = FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 19)
    override val dateOfBirthCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 19, endInLineExclusive = 20)
    override val sex: FieldSpec = FieldSpec(line = 1, startInLine = 20, endInLineExclusive = 21)
    override val dateOfExpiry: FieldSpec = FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 27)
    override val dateOfExpiryCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 27, endInLineExclusive = 28)
    public val personalNumber: FieldSpec = FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 42)
    public val personalNumberCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 42, endInLineExclusive = 43)
    override val compositeCheckDigit: FieldSpec = FieldSpec(line = 1, startInLine = 43, endInLineExclusive = 44)

    // Per ICAO Doc 9303 Part 4: composite check digit covers document number + its check
    // digit, date of birth + its check digit, and date of expiry + its check digit + personal
    // number + its check digit (sex and the composite digit itself are excluded).
    override val compositeInputFields: List<FieldSpec> =
        listOf(
            FieldSpec(line = 1, startInLine = 0, endInLineExclusive = 10),
            FieldSpec(line = 1, startInLine = 13, endInLineExclusive = 20),
            FieldSpec(line = 1, startInLine = 21, endInLineExclusive = 43),
        )
}
