package io.lightine.tessera.mrz.formats

/**
 * The shared contract for every ICAO Doc 9303 MRZ format specification.
 *
 * Each implementing object names the field positions for one format and provides the
 * arithmetic for converting line-relative [FieldSpec] coordinates into the global position
 * in the concatenated MRZ used for error reporting.
 *
 * The fields below are present in every supported format. Formats that also define a
 * composite check digit (TD1, TD2, TD3 per ICAO Doc 9303 Parts 4-6) implement the
 * [MrzFormatSpecWithComposite] sub-interface that adds the composite-related fields.
 * Visa formats (MRV-A, MRV-B per Part 7) implement this base interface only.
 *
 * Sealed to the `mrz-core` module — adding a new format spec requires declaring it within
 * this module so the closed set matches the sealed [io.lightine.tessera.mrz.model.MrzDocument]
 * hierarchy.
 */
public sealed interface MrzFormatSpec {
    public val lineCount: Int
    public val lineLength: Int

    public val documentType: FieldSpec
    public val issuingState: FieldSpec
    public val rawNameField: FieldSpec
    public val documentNumber: FieldSpec
    public val documentNumberCheckDigit: FieldSpec
    public val nationality: FieldSpec
    public val dateOfBirth: FieldSpec
    public val dateOfBirthCheckDigit: FieldSpec
    public val sex: FieldSpec
    public val dateOfExpiry: FieldSpec
    public val dateOfExpiryCheckDigit: FieldSpec

    /**
     * Convert a line-relative [FieldSpec] into the position in the concatenated MRZ string
     * (line 0 occupies positions `[0, lineLength)`, line 1 occupies `[lineLength, 2 * lineLength)`,
     * and so on). Used in error reporting so consumers can point at the failing character.
     */
    public fun globalPositionOf(field: FieldSpec): Int = field.line * lineLength + field.startInLine
}

/**
 * Extension of [MrzFormatSpec] for formats that define a composite check digit per ICAO Doc 9303.
 *
 * Implemented by TD1 (Part 5), TD2 (Part 6), and TD3 (Part 4). Visa formats (MRV-A, MRV-B per
 * Part 7) do NOT define a composite check digit and therefore implement only the base
 * [MrzFormatSpec], not this sub-interface.
 *
 * The `compositeInputFields` list names the field ranges that concatenate to form the input
 * to the composite check digit computation; the exact ranges differ per format and are
 * documented on the implementing object.
 */
public sealed interface MrzFormatSpecWithComposite : MrzFormatSpec {
    public val compositeCheckDigit: FieldSpec
    public val compositeInputFields: List<FieldSpec>
}
