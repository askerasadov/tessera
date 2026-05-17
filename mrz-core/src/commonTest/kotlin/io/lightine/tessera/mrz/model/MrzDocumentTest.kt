package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.MrzFormat
import io.lightine.tessera.domain.vocabulary.Sex
import io.lightine.tessera.mrz.recognition.CountryCode
import io.lightine.tessera.mrz.recognition.DocumentType
import kotlin.test.Test
import kotlin.test.assertEquals

class MrzDocumentTest {
    private val specimenCommonFields =
        CommonFields(
            documentType = DocumentType("P"),
            issuingState = CountryCode("UTO"),
            primaryIdentifier = "ERIKSSON",
            secondaryIdentifier = "ANNA MARIA",
            nameTruncated = false,
            rawNameField = "ERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
            documentNumber = "L898902C<",
            nationality = CountryCode("UTO"),
            dateOfBirth = MrzDate(rawYear = "69", rawMonth = "08", rawDay = "06"),
            sex = Sex.FEMALE,
            rawSex = 'F',
            dateOfExpiry = MrzDate(rawYear = "94", rawMonth = "06", rawDay = "23"),
            checkDigits =
                MrzCheckDigits(
                    documentNumber = '3',
                    dateOfBirth = '1',
                    dateOfExpiry = '6',
                    optionalData = '1',
                    composite = '4',
                ),
        )

    private val specimenTd3 =
        TD3(
            rawLines =
                listOf(
                    "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
                    "L898902C<3UTO6908061F9406236ZE184226B<<<<<14",
                ),
            commonFields = specimenCommonFields,
            personalNumber = "ZE184226B<<<<<",
            personalNumberCheckDigit = '1',
        )

    @Test
    fun td3_specimen_constructs_and_exposes_common_fields_verbatim() {
        assertEquals("P", specimenTd3.commonFields.documentType.rawCode)
        assertEquals("UTO", specimenTd3.commonFields.issuingState.rawCode)
        assertEquals("ERIKSSON", specimenTd3.commonFields.primaryIdentifier)
        assertEquals("ANNA MARIA", specimenTd3.commonFields.secondaryIdentifier)
        assertEquals(false, specimenTd3.commonFields.nameTruncated)
        assertEquals("L898902C<", specimenTd3.commonFields.documentNumber)
        assertEquals("UTO", specimenTd3.commonFields.nationality.rawCode)
        assertEquals(Sex.FEMALE, specimenTd3.commonFields.sex)
    }

    @Test
    fun td3_specimen_exposes_dates_as_raw_year_month_day() {
        val dob = specimenTd3.commonFields.dateOfBirth
        assertEquals("69", dob.rawYear)
        assertEquals("08", dob.rawMonth)
        assertEquals("06", dob.rawDay)

        val expiry = specimenTd3.commonFields.dateOfExpiry
        assertEquals("94", expiry.rawYear)
        assertEquals("06", expiry.rawMonth)
        assertEquals("23", expiry.rawDay)
    }

    @Test
    fun td3_specimen_exposes_check_digits_verbatim() {
        val checks = specimenTd3.commonFields.checkDigits
        assertEquals('3', checks.documentNumber)
        assertEquals('1', checks.dateOfBirth)
        assertEquals('6', checks.dateOfExpiry)
        assertEquals('1', checks.optionalData)
        assertEquals('4', checks.composite)
    }

    @Test
    fun td3_specimen_exposes_personal_number_and_its_check_digit() {
        assertEquals("ZE184226B<<<<<", specimenTd3.personalNumber)
        assertEquals('1', specimenTd3.personalNumberCheckDigit)
    }

    @Test
    fun td3_format_is_always_td3() {
        assertEquals(MrzFormat.TD3, specimenTd3.format)
    }

    @Test
    fun td3_raw_lines_round_trip_unchanged() {
        assertEquals(
            listOf(
                "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
                "L898902C<3UTO6908061F9406236ZE184226B<<<<<14",
            ),
            specimenTd3.rawLines,
        )
    }

    @Test
    fun two_td3_with_identical_field_values_are_equal() {
        val other =
            TD3(
                rawLines = specimenTd3.rawLines,
                commonFields = specimenCommonFields,
                personalNumber = "ZE184226B<<<<<",
                personalNumberCheckDigit = '1',
            )
        assertEquals(specimenTd3, other)
    }

    @Test
    fun sealed_hierarchy_supports_exhaustive_when_matching_across_known_variants() {
        val td1: MrzDocument =
            TD1(
                rawLines = listOf("", "", ""),
                commonFields = specimenCommonFields,
                optionalData1 = "",
                optionalData2 = "",
            )
        val td3: MrzDocument = specimenTd3

        val labels =
            listOf(td1, td3).map { doc ->
                when (doc) {
                    is TD1 -> "TD1"
                    is TD3 -> "TD3"
                }
            }
        assertEquals(listOf("TD1", "TD3"), labels)
    }
}
