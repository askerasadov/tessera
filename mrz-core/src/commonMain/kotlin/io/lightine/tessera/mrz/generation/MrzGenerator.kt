package io.lightine.tessera.mrz.generation

import io.lightine.tessera.domain.errors.MrzGenerationFieldOverflow
import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat
import io.lightine.tessera.domain.vocabulary.ReadMethod
import io.lightine.tessera.mrz.checkdigit.computeCheckDigit
import io.lightine.tessera.mrz.formats.Td3FormatSpec
import io.lightine.tessera.mrz.formats.extractFrom
import io.lightine.tessera.mrz.model.TD3
import io.lightine.tessera.mrz.parsing.ResultMetadata

public object MrzGenerator {
    public fun generate(document: TD3): GenerationResult {
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

        // Line 1: document type (2) + issuing state (3) + name field (39) = 44 chars
        val docType =
            document.commonFields.documentType.rawCode
                .padEnd(Td3FormatSpec.documentType.width, '<')
        val state =
            document.commonFields.issuingState.rawCode
                .padEnd(Td3FormatSpec.issuingState.width, '<')
        val name = document.commonFields.rawNameField.padEnd(Td3FormatSpec.rawNameField.width, '<')
        val line1 = docType + state + name

        // Line 2 minus the trailing composite check digit. Per ICAO Doc 9303 Part 4, check digits
        // are recomputed from the field data — the parser-extracted values on the input document
        // are not used (Principle 7: strict conformance over faithful round-trip of bad inputs).
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

        // Compute composite using the same FieldSpec ranges the parser/validator use, ensuring
        // generator and validator agree on the composite-input definition by construction.
        // The composite slot is the final character of line 2; we pad with a placeholder to
        // make extractFrom work, then replace.
        val placeholderLines = listOf(line1, line2Prefix + '<')
        val compositeInput = Td3FormatSpec.compositeInputFields.joinToString("") { it.extractFrom(placeholderLines) }
        val composite = computeCheckDigit(compositeInput)
        val line2 = line2Prefix + composite

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
