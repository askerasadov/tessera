package io.lightine.tessera.mrz.generation

import io.lightine.tessera.domain.errors.MrzGenerationFieldOverflow
import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat
import io.lightine.tessera.domain.vocabulary.ReadMethod
import io.lightine.tessera.mrz.checkdigit.computeCheckDigit
import io.lightine.tessera.mrz.formats.MrvAFormatSpec
import io.lightine.tessera.mrz.formats.MrvBFormatSpec
import io.lightine.tessera.mrz.formats.Td1FormatSpec
import io.lightine.tessera.mrz.formats.Td2FormatSpec
import io.lightine.tessera.mrz.formats.Td3FormatSpec
import io.lightine.tessera.mrz.formats.extractFrom
import io.lightine.tessera.mrz.model.MrvA
import io.lightine.tessera.mrz.model.MrvB
import io.lightine.tessera.mrz.model.MrzDocument
import io.lightine.tessera.mrz.model.TD1
import io.lightine.tessera.mrz.model.TD2
import io.lightine.tessera.mrz.model.TD3
import io.lightine.tessera.mrz.parsing.ResultMetadata

public object MrzGenerator {
    /**
     * Polymorphic dispatch over the [MrzDocument] sealed hierarchy. Consumers holding a typed
     * `MrzDocument` reference (typically from `MrzParser.parse(input).document`) can call this
     * directly without narrowing to the specific variant.
     */
    public fun generate(document: MrzDocument): GenerationResult =
        when (document) {
            is TD1 -> generate(document)
            is TD2 -> generate(document)
            is TD3 -> generate(document)
            is MrvA -> generate(document)
            is MrvB -> generate(document)
        }

    public fun generate(document: TD3): GenerationResult = generateTd3(document)

    public fun generate(document: TD2): GenerationResult = generateTd2(document)

    public fun generate(document: TD1): GenerationResult = generateTd1(document)

    public fun generate(document: MrvA): GenerationResult = generateMrvA(document)

    public fun generate(document: MrvB): GenerationResult = generateMrvB(document)

    // ----------------------------------------------------------------
    // Per-format generation paths
    //
    // Each path validates field widths against the format spec, pads short fields with the
    // filler character `<`, recomputes every check digit from the field data, and assembles
    // the lines per ICAO Doc 9303 Parts 4–7. The composite check digit (where the format
    // defines one) is computed via the same `compositeInputFields` ranges the parser and
    // validator consume — generator, parser, and validator agree on the composite-input
    // definition by construction.
    // ----------------------------------------------------------------

    private fun generateTd3(document: TD3): GenerationResult {
        validateLength(
            MrzFormat.TD3,
            MrzField.DOCUMENT_TYPE,
            document.commonFields.documentType.rawCode,
            Td3FormatSpec.documentType.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD3,
            MrzField.ISSUING_STATE,
            document.commonFields.issuingState.rawCode,
            Td3FormatSpec.issuingState.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD3,
            MrzField.NAME_FIELD,
            document.commonFields.rawNameField,
            Td3FormatSpec.rawNameField.width,
        )?.let { return it }
        validateLength(
            MrzFormat.TD3,
            MrzField.DOCUMENT_NUMBER,
            document.commonFields.documentNumber,
            Td3FormatSpec.documentNumber.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD3,
            MrzField.NATIONALITY,
            document.commonFields.nationality.rawCode,
            Td3FormatSpec.nationality.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD3,
            MrzField.OPTIONAL_DATA,
            document.personalNumber,
            Td3FormatSpec.personalNumber.width,
        )?.let { return it }

        val docType =
            document.commonFields.documentType.rawCode
                .padEnd(Td3FormatSpec.documentType.width, '<')
        val state =
            document.commonFields.issuingState.rawCode
                .padEnd(Td3FormatSpec.issuingState.width, '<')
        val name = document.commonFields.rawNameField.padEnd(Td3FormatSpec.rawNameField.width, '<')
        val line1 = docType + state + name

        val docNum = document.commonFields.documentNumber.padEnd(Td3FormatSpec.documentNumber.width, '<')
        val docNumCheck = computeCheckDigit(docNum)
        val nationality =
            document.commonFields.nationality.rawCode
                .padEnd(Td3FormatSpec.nationality.width, '<')
        val dob = document.commonFields.dateOfBirth.run { rawYear + rawMonth + rawDay }
        val dobCheck = computeCheckDigit(dob)
        val sex = document.commonFields.rawSex.toString()
        val doe = document.commonFields.dateOfExpiry.run { rawYear + rawMonth + rawDay }
        val doeCheck = computeCheckDigit(doe)
        val personalNum = document.personalNumber.padEnd(Td3FormatSpec.personalNumber.width, '<')
        val personalNumCheck = computeCheckDigit(personalNum)

        val line2Prefix =
            docNum + docNumCheck + nationality +
                dob + dobCheck + sex +
                doe + doeCheck +
                personalNum + personalNumCheck

        val placeholderLines = listOf(line1, line2Prefix + '<')
        val compositeInput = Td3FormatSpec.compositeInputFields.joinToString("") { it.extractFrom(placeholderLines) }
        val composite = computeCheckDigit(compositeInput)
        val line2 = line2Prefix + composite

        return GenerationResult.Success(mrz = listOf(line1, line2), metadata = emptyMetadata())
    }

    private fun generateTd2(document: TD2): GenerationResult {
        validateLength(
            MrzFormat.TD2,
            MrzField.DOCUMENT_TYPE,
            document.commonFields.documentType.rawCode,
            Td2FormatSpec.documentType.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD2,
            MrzField.ISSUING_STATE,
            document.commonFields.issuingState.rawCode,
            Td2FormatSpec.issuingState.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD2,
            MrzField.NAME_FIELD,
            document.commonFields.rawNameField,
            Td2FormatSpec.rawNameField.width,
        )?.let { return it }
        validateLength(
            MrzFormat.TD2,
            MrzField.DOCUMENT_NUMBER,
            document.commonFields.documentNumber,
            Td2FormatSpec.documentNumber.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD2,
            MrzField.NATIONALITY,
            document.commonFields.nationality.rawCode,
            Td2FormatSpec.nationality.width,
        )?.let {
            return it
        }
        validateLength(MrzFormat.TD2, MrzField.OPTIONAL_DATA, document.optionalData, Td2FormatSpec.optionalData.width)?.let { return it }

        val docType =
            document.commonFields.documentType.rawCode
                .padEnd(Td2FormatSpec.documentType.width, '<')
        val state =
            document.commonFields.issuingState.rawCode
                .padEnd(Td2FormatSpec.issuingState.width, '<')
        val name = document.commonFields.rawNameField.padEnd(Td2FormatSpec.rawNameField.width, '<')
        val line1 = docType + state + name

        // TD2 has no per-field optional-data check digit (Part 6). Composite covers DOE+check+optionalData.
        val docNum = document.commonFields.documentNumber.padEnd(Td2FormatSpec.documentNumber.width, '<')
        val docNumCheck = computeCheckDigit(docNum)
        val nationality =
            document.commonFields.nationality.rawCode
                .padEnd(Td2FormatSpec.nationality.width, '<')
        val dob = document.commonFields.dateOfBirth.run { rawYear + rawMonth + rawDay }
        val dobCheck = computeCheckDigit(dob)
        val sex = document.commonFields.rawSex.toString()
        val doe = document.commonFields.dateOfExpiry.run { rawYear + rawMonth + rawDay }
        val doeCheck = computeCheckDigit(doe)
        val optionalData = document.optionalData.padEnd(Td2FormatSpec.optionalData.width, '<')

        val line2Prefix =
            docNum + docNumCheck + nationality +
                dob + dobCheck + sex +
                doe + doeCheck +
                optionalData

        val placeholderLines = listOf(line1, line2Prefix + '<')
        val compositeInput = Td2FormatSpec.compositeInputFields.joinToString("") { it.extractFrom(placeholderLines) }
        val composite = computeCheckDigit(compositeInput)
        val line2 = line2Prefix + composite

        return GenerationResult.Success(mrz = listOf(line1, line2), metadata = emptyMetadata())
    }

    private fun generateTd1(document: TD1): GenerationResult {
        validateLength(
            MrzFormat.TD1,
            MrzField.DOCUMENT_TYPE,
            document.commonFields.documentType.rawCode,
            Td1FormatSpec.documentType.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD1,
            MrzField.ISSUING_STATE,
            document.commonFields.issuingState.rawCode,
            Td1FormatSpec.issuingState.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.TD1,
            MrzField.DOCUMENT_NUMBER,
            document.commonFields.documentNumber,
            Td1FormatSpec.documentNumber.width,
        )?.let {
            return it
        }
        validateLength(MrzFormat.TD1, MrzField.OPTIONAL_DATA, document.optionalData1, Td1FormatSpec.optionalData1.width)?.let { return it }
        validateLength(
            MrzFormat.TD1,
            MrzField.NATIONALITY,
            document.commonFields.nationality.rawCode,
            Td1FormatSpec.nationality.width,
        )?.let {
            return it
        }
        validateLength(MrzFormat.TD1, MrzField.OPTIONAL_DATA, document.optionalData2, Td1FormatSpec.optionalData2.width)?.let { return it }
        validateLength(
            MrzFormat.TD1,
            MrzField.NAME_FIELD,
            document.commonFields.rawNameField,
            Td1FormatSpec.rawNameField.width,
        )?.let { return it }

        // Line 1: docType(2) + state(3) + docNum(9) + docCheck(1) + optionalData1(15) = 30
        val docType =
            document.commonFields.documentType.rawCode
                .padEnd(Td1FormatSpec.documentType.width, '<')
        val state =
            document.commonFields.issuingState.rawCode
                .padEnd(Td1FormatSpec.issuingState.width, '<')
        val docNum = document.commonFields.documentNumber.padEnd(Td1FormatSpec.documentNumber.width, '<')
        val docNumCheck = computeCheckDigit(docNum)
        val optionalData1 = document.optionalData1.padEnd(Td1FormatSpec.optionalData1.width, '<')
        val line1 = docType + state + docNum + docNumCheck + optionalData1

        // Line 2 prefix: dob(6) + dobCheck(1) + sex(1) + doe(6) + doeCheck(1) + nat(3) + optionalData2(11) = 29
        val dob = document.commonFields.dateOfBirth.run { rawYear + rawMonth + rawDay }
        val dobCheck = computeCheckDigit(dob)
        val sex = document.commonFields.rawSex.toString()
        val doe = document.commonFields.dateOfExpiry.run { rawYear + rawMonth + rawDay }
        val doeCheck = computeCheckDigit(doe)
        val nationality =
            document.commonFields.nationality.rawCode
                .padEnd(Td1FormatSpec.nationality.width, '<')
        val optionalData2 = document.optionalData2.padEnd(Td1FormatSpec.optionalData2.width, '<')
        val line2Prefix = dob + dobCheck + sex + doe + doeCheck + nationality + optionalData2

        // Line 3: name field (30 chars)
        val line3 = document.commonFields.rawNameField.padEnd(Td1FormatSpec.rawNameField.width, '<')

        // Composite input spans line 1 [5,30) (doc num + check + optionalData1), line 2 [0,7)
        // (dob + dobCheck), line 2 [8,15) (doe + doeCheck), line 2 [18,29) (optionalData2).
        val placeholderLines = listOf(line1, line2Prefix + '<', line3)
        val compositeInput = Td1FormatSpec.compositeInputFields.joinToString("") { it.extractFrom(placeholderLines) }
        val composite = computeCheckDigit(compositeInput)
        val line2 = line2Prefix + composite

        return GenerationResult.Success(mrz = listOf(line1, line2, line3), metadata = emptyMetadata())
    }

    private fun generateMrvA(document: MrvA): GenerationResult {
        validateLength(
            MrzFormat.MRV_A,
            MrzField.DOCUMENT_TYPE,
            document.commonFields.documentType.rawCode,
            MrvAFormatSpec.documentType.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.MRV_A,
            MrzField.ISSUING_STATE,
            document.commonFields.issuingState.rawCode,
            MrvAFormatSpec.issuingState.width,
        )?.let {
            return it
        }
        validateLength(MrzFormat.MRV_A, MrzField.NAME_FIELD, document.commonFields.rawNameField, MrvAFormatSpec.rawNameField.width)?.let {
            return it
        }
        validateLength(
            MrzFormat.MRV_A,
            MrzField.DOCUMENT_NUMBER,
            document.commonFields.documentNumber,
            MrvAFormatSpec.documentNumber.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.MRV_A,
            MrzField.NATIONALITY,
            document.commonFields.nationality.rawCode,
            MrvAFormatSpec.nationality.width,
        )?.let {
            return it
        }
        validateLength(MrzFormat.MRV_A, MrzField.OPTIONAL_DATA, document.optionalData, MrvAFormatSpec.optionalData.width)?.let { return it }

        val docType =
            document.commonFields.documentType.rawCode
                .padEnd(MrvAFormatSpec.documentType.width, '<')
        val state =
            document.commonFields.issuingState.rawCode
                .padEnd(MrvAFormatSpec.issuingState.width, '<')
        val name = document.commonFields.rawNameField.padEnd(MrvAFormatSpec.rawNameField.width, '<')
        val line1 = docType + state + name

        // MRV-A has no composite check digit (Part 7); line 2 ends with the optional-data tail.
        val docNum = document.commonFields.documentNumber.padEnd(MrvAFormatSpec.documentNumber.width, '<')
        val docNumCheck = computeCheckDigit(docNum)
        val nationality =
            document.commonFields.nationality.rawCode
                .padEnd(MrvAFormatSpec.nationality.width, '<')
        val dob = document.commonFields.dateOfBirth.run { rawYear + rawMonth + rawDay }
        val dobCheck = computeCheckDigit(dob)
        val sex = document.commonFields.rawSex.toString()
        val doe = document.commonFields.dateOfExpiry.run { rawYear + rawMonth + rawDay }
        val doeCheck = computeCheckDigit(doe)
        val optionalData = document.optionalData.padEnd(MrvAFormatSpec.optionalData.width, '<')

        val line2 =
            docNum + docNumCheck + nationality +
                dob + dobCheck + sex +
                doe + doeCheck +
                optionalData

        return GenerationResult.Success(mrz = listOf(line1, line2), metadata = emptyMetadata())
    }

    private fun generateMrvB(document: MrvB): GenerationResult {
        validateLength(
            MrzFormat.MRV_B,
            MrzField.DOCUMENT_TYPE,
            document.commonFields.documentType.rawCode,
            MrvBFormatSpec.documentType.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.MRV_B,
            MrzField.ISSUING_STATE,
            document.commonFields.issuingState.rawCode,
            MrvBFormatSpec.issuingState.width,
        )?.let {
            return it
        }
        validateLength(MrzFormat.MRV_B, MrzField.NAME_FIELD, document.commonFields.rawNameField, MrvBFormatSpec.rawNameField.width)?.let {
            return it
        }
        validateLength(
            MrzFormat.MRV_B,
            MrzField.DOCUMENT_NUMBER,
            document.commonFields.documentNumber,
            MrvBFormatSpec.documentNumber.width,
        )?.let {
            return it
        }
        validateLength(
            MrzFormat.MRV_B,
            MrzField.NATIONALITY,
            document.commonFields.nationality.rawCode,
            MrvBFormatSpec.nationality.width,
        )?.let {
            return it
        }
        validateLength(MrzFormat.MRV_B, MrzField.OPTIONAL_DATA, document.optionalData, MrvBFormatSpec.optionalData.width)?.let { return it }

        val docType =
            document.commonFields.documentType.rawCode
                .padEnd(MrvBFormatSpec.documentType.width, '<')
        val state =
            document.commonFields.issuingState.rawCode
                .padEnd(MrvBFormatSpec.issuingState.width, '<')
        val name = document.commonFields.rawNameField.padEnd(MrvBFormatSpec.rawNameField.width, '<')
        val line1 = docType + state + name

        // MRV-B has no composite check digit (Part 7); line 2 ends with the optional-data tail.
        val docNum = document.commonFields.documentNumber.padEnd(MrvBFormatSpec.documentNumber.width, '<')
        val docNumCheck = computeCheckDigit(docNum)
        val nationality =
            document.commonFields.nationality.rawCode
                .padEnd(MrvBFormatSpec.nationality.width, '<')
        val dob = document.commonFields.dateOfBirth.run { rawYear + rawMonth + rawDay }
        val dobCheck = computeCheckDigit(dob)
        val sex = document.commonFields.rawSex.toString()
        val doe = document.commonFields.dateOfExpiry.run { rawYear + rawMonth + rawDay }
        val doeCheck = computeCheckDigit(doe)
        val optionalData = document.optionalData.padEnd(MrvBFormatSpec.optionalData.width, '<')

        val line2 =
            docNum + docNumCheck + nationality +
                dob + dobCheck + sex +
                doe + doeCheck +
                optionalData

        return GenerationResult.Success(mrz = listOf(line1, line2), metadata = emptyMetadata())
    }

    private fun validateLength(
        format: MrzFormat,
        field: MrzField,
        value: String,
        maxLength: Int,
    ): GenerationResult.Failure? {
        if (value.length <= maxLength) return null
        return GenerationResult.Failure(
            error =
                MrzGenerationFieldOverflow(
                    format = format,
                    field = field,
                    maxLength = maxLength,
                    observedLength = value.length,
                    observedValue = value,
                ),
            metadata = emptyMetadata(),
        )
    }

    // Pragmatic placeholder: `ReadMethod` was designed for parser results (which read data from
    // some source) but `GenerationResult` carries it too because `ResultMetadata` is shared.
    // BACKEND_STRING_INPUT is the closest fit pre-0.1.0. A future refinement may either widen
    // `ReadMethod` (e.g., `GENERATED`) or split `ResultMetadata` into parse- and generate-
    // specific variants. Tracked in the next session handoff.
    private fun emptyMetadata(): ResultMetadata =
        ResultMetadata(
            readMethod = ReadMethod.BACKEND_STRING_INPUT,
            warnings = emptyList(),
            validationFailures = emptyList(),
        )
}
