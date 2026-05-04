package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzCharacterSetViolation
import io.lightine.tessera.domain.MrzFormat
import io.lightine.tessera.domain.MrzInvalidLength
import io.lightine.tessera.domain.ReadMethod
import io.lightine.tessera.domain.Sex
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

public object MrzParser {
    private const val TD3_LINE_COUNT = 2
    private const val TD3_LINE_LENGTH = 44

    public fun parseTD3(
        input: String,
        referenceTime: Instant = Clock.System.now(),
    ): ParseResult = parseTD3(splitLines(input), referenceTime)

    public fun parseTD3(
        input: List<String>,
        referenceTime: Instant = Clock.System.now(),
    ): ParseResult {
        val emptyMetadata =
            ResultMetadata(
                readMethod = ReadMethod.BACKEND_STRING_INPUT,
                warnings = emptyList(),
                validationFailures = emptyList(),
            )

        validateLineShape(input)?.let { error ->
            return ParseResult.Failure(error = error, rawInput = input.joinToString("\n"), metadata = emptyMetadata)
        }

        validateAlphabet(input)?.let { violation ->
            return ParseResult.Failure(error = violation, rawInput = input.joinToString("\n"), metadata = emptyMetadata)
        }

        val td3 = sliceTd3Fields(input, referenceTime)
        return ParseResult.Success(document = td3, metadata = emptyMetadata)
    }

    private fun validateLineShape(input: List<String>): MrzInvalidLength? {
        val observedLengths = input.map { it.length }
        val countMatches = input.size == TD3_LINE_COUNT
        val lengthsMatch = observedLengths.all { it == TD3_LINE_LENGTH }
        return if (countMatches && lengthsMatch) {
            null
        } else {
            MrzInvalidLength(
                format = MrzFormat.TD3,
                expectedLineCount = TD3_LINE_COUNT,
                expectedLineLength = TD3_LINE_LENGTH,
                observedLineCount = input.size,
                observedLineLengths = observedLengths,
            )
        }
    }

    private fun validateAlphabet(input: List<String>): MrzCharacterSetViolation? {
        for ((lineIndex, line) in input.withIndex()) {
            for ((charIndex, c) in line.withIndex()) {
                if (!isMrzAlphabetCharacter(c)) {
                    return MrzCharacterSetViolation(
                        offendingCharacter = c,
                        position = lineIndex * TD3_LINE_LENGTH + charIndex,
                    )
                }
            }
        }
        return null
    }

    private fun sliceTd3Fields(
        input: List<String>,
        referenceTime: Instant,
    ): TD3 {
        val line1 = input[0]
        val line2 = input[1]

        val documentTypeCode = line1.substring(0, 2).trimEnd('<')
        val issuingState = line1.substring(2, 5)
        val rawNameField = line1.substring(5, 44)

        val documentNumber = line2.substring(0, 9)
        val docNumberCheckDigit = line2[9]
        val nationality = line2.substring(10, 13)
        val rawDob = line2.substring(13, 19)
        val dobCheckDigit = line2[19]
        val sexChar = line2[20]
        val rawExpiry = line2.substring(21, 27)
        val expiryCheckDigit = line2[27]
        val personalNumber = line2.substring(28, 42)
        val personalNumberCheckDigit = line2[42]
        val compositeCheckDigit = line2[43]

        val sex =
            when (sexChar) {
                'M' -> Sex.MALE
                'F' -> Sex.FEMALE
                else -> Sex.UNSPECIFIED
            }

        val dateOfBirth =
            MrzDate.parseBirth(
                rawYear = rawDob.substring(0, 2),
                rawMonth = rawDob.substring(2, 4),
                rawDay = rawDob.substring(4, 6),
                referenceTime = referenceTime,
            )

        val dateOfExpiry =
            MrzDate.parseExpiry(
                rawYear = rawExpiry.substring(0, 2),
                rawMonth = rawExpiry.substring(2, 4),
                rawDay = rawExpiry.substring(4, 6),
                referenceTime = referenceTime,
            )

        val checkDigits =
            MrzCheckDigits(
                documentNumber = docNumberCheckDigit,
                dateOfBirth = dobCheckDigit,
                dateOfExpiry = expiryCheckDigit,
                optionalData = personalNumberCheckDigit,
                composite = compositeCheckDigit,
            )

        val commonFields =
            CommonFields(
                documentType = DocumentType(documentTypeCode),
                issuingState = issuingState,
                primaryIdentifier = "",
                secondaryIdentifier = "",
                nameTruncated = false,
                rawNameField = rawNameField,
                documentNumber = documentNumber,
                nationality = nationality,
                dateOfBirth = dateOfBirth,
                sex = sex,
                dateOfExpiry = dateOfExpiry,
                checkDigits = checkDigits,
            )

        return TD3(
            rawLines = input,
            commonFields = commonFields,
            personalNumber = personalNumber,
            personalNumberCheckDigit = personalNumberCheckDigit,
        )
    }

    private fun splitLines(input: String): List<String> = input.trimEnd().lines().dropWhile { it.isEmpty() }
}
