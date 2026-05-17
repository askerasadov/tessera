package io.lightine.tessera.mrz.formats

public object MrvBFormatSpec : MrzFormatSpec {
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
    public val optionalData: FieldSpec = FieldSpec(line = 1, startInLine = 28, endInLineExclusive = 36)

    // MRV-B has no composite check digit per ICAO Doc 9303 Part 7: the last 8 characters of
    // line 2 are entirely optional data. The spec implements `MrzFormatSpec` (not
    // `MrzFormatSpecWithComposite`), so `compositeCheckDigit` and `compositeInputFields` are
    // absent from this object's API surface.
}
