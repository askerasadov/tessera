package io.lightine.tessera.mrz

import io.lightine.tessera.domain.MrzFormat
import io.lightine.tessera.domain.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TD1Test {
    private val syntheticCommonFields =
        CommonFields(
            documentType = DocumentType("I"),
            issuingState = "UTO",
            primaryIdentifier = "ERIKSSON",
            secondaryIdentifier = "ANNA MARIA",
            nameTruncated = false,
            rawNameField = "ERIKSSON<<ANNA<MARIA<<<<<<<<<<",
            documentNumber = "L898902C<",
            nationality = "UTO",
            dateOfBirth = MrzDate(rawYear = "69", rawMonth = "08", rawDay = "06"),
            sex = Sex.FEMALE,
            dateOfExpiry = MrzDate(rawYear = "30", rawMonth = "08", rawDay = "06"),
            checkDigits =
                MrzCheckDigits(
                    documentNumber = '0',
                    dateOfBirth = '1',
                    dateOfExpiry = '0',
                    optionalData = null,
                    composite = '0',
                ),
        )

    private val syntheticTd1 =
        TD1(
            rawLines =
                listOf(
                    "I<UTOL898902C<0<<<<<<<<<<<<<<<",
                    "6908061F3008060UTO<<<<<<<<<<<0",
                    "ERIKSSON<<ANNA<MARIA<<<<<<<<<<",
                ),
            commonFields = syntheticCommonFields,
            optionalData1 = "<<<<<<<<<<<<<<<",
            optionalData2 = "<<<<<<<<<<<",
        )

    @Test
    fun td1_synthetic_constructs_and_exposes_common_fields_verbatim() {
        assertEquals("I", syntheticTd1.commonFields.documentType.rawCode)
        assertEquals("UTO", syntheticTd1.commonFields.issuingState)
        assertEquals("ERIKSSON", syntheticTd1.commonFields.primaryIdentifier)
        assertEquals("ANNA MARIA", syntheticTd1.commonFields.secondaryIdentifier)
        assertEquals("L898902C<", syntheticTd1.commonFields.documentNumber)
        assertEquals(Sex.FEMALE, syntheticTd1.commonFields.sex)
    }

    @Test
    fun td1_exposes_both_optional_data_fields() {
        assertEquals("<<<<<<<<<<<<<<<", syntheticTd1.optionalData1)
        assertEquals("<<<<<<<<<<<", syntheticTd1.optionalData2)
    }

    @Test
    fun td1_check_digits_have_no_optional_data_position() {
        // TD1 does not define a separate optional-data check digit position;
        // MrzCheckDigits.optionalData is null for this format.
        assertNull(syntheticTd1.commonFields.checkDigits.optionalData)
    }

    @Test
    fun td1_format_is_always_td1() {
        assertEquals(MrzFormat.TD1, syntheticTd1.format)
    }

    @Test
    fun td1_raw_lines_round_trip_unchanged() {
        assertEquals(
            listOf(
                "I<UTOL898902C<0<<<<<<<<<<<<<<<",
                "6908061F3008060UTO<<<<<<<<<<<0",
                "ERIKSSON<<ANNA<MARIA<<<<<<<<<<",
            ),
            syntheticTd1.rawLines,
        )
    }

    @Test
    fun td1_raw_lines_are_three_lines_of_thirty_characters() {
        assertEquals(3, syntheticTd1.rawLines.size)
        syntheticTd1.rawLines.forEach { line ->
            assertEquals(30, line.length, "Each TD1 line should be 30 characters; got '$line'")
        }
    }

    @Test
    fun two_td1_with_identical_field_values_are_equal() {
        val other =
            TD1(
                rawLines = syntheticTd1.rawLines,
                commonFields = syntheticCommonFields,
                optionalData1 = "<<<<<<<<<<<<<<<",
                optionalData2 = "<<<<<<<<<<<",
            )
        assertEquals(syntheticTd1, other)
    }
}
