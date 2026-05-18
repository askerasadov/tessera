package io.lightine.tessera.mrz.generation

import io.lightine.tessera.domain.errors.MrzGenerationNumericInNameField
import io.lightine.tessera.domain.errors.MrzGenerationUnsupportedCharacters
import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat
import io.lightine.tessera.domain.vocabulary.Sex
import io.lightine.tessera.mrz.model.MrvA
import io.lightine.tessera.mrz.model.MrvB
import io.lightine.tessera.mrz.model.TD1
import io.lightine.tessera.mrz.model.TD2
import io.lightine.tessera.mrz.model.TD3
import io.lightine.tessera.mrz.parsing.MrzParser
import io.lightine.tessera.mrz.parsing.ParseResult
import io.lightine.tessera.mrz.transliteration.AzeTransliterationProfile
import io.lightine.tessera.mrz.transliteration.IcaoDefaultTransliterationProfile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class MrzGeneratorPrimitiveInputTest {
    private val ref2026 = Instant.parse("2026-05-04T12:00:00Z")
    private val dob = LocalDate(1969, 8, 6)
    private val doe = LocalDate(1994, 6, 23)

    // ---------- TD3 ----------

    @Test
    fun td3_primitive_input_round_trips_clean_ascii_to_specimen_lines() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "ANNA MARIA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                personalNumber = "ZE184226B",
            )
        val success = assertIs<GenerationResult.Success>(result)
        assertEquals(
            listOf(
                "P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<",
                "L898902C<3UTO6908061F9406236ZE184226B<<<<<14",
            ),
            success.mrz,
        )
        // No transliteration was applied; details should be null.
        assertNull(success.metadata.transliterationDetails)
    }

    @Test
    fun td3_primitive_input_round_trips_via_parser() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "ANNA MARIA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                personalNumber = "ZE184226B",
            )
        val success = assertIs<GenerationResult.Success>(result)
        val parsed = MrzParser.parseTD3(success.mrz, referenceTime = ref2026)
        val parseSuccess = assertIs<ParseResult.Success>(parsed)
        val td3 = assertIs<TD3>(parseSuccess.document)
        assertEquals("ERIKSSON", td3.commonFields.primaryIdentifier)
        assertEquals("ANNA MARIA", td3.commonFields.secondaryIdentifier)
        assertEquals("L898902C<", td3.commonFields.documentNumber)
        assertEquals("ZE184226B", td3.personalNumber.trimEnd('<'))
    }

    @Test
    fun td3_with_icao_profile_transliterates_diacritic_input_and_surfaces_details() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "DEU",
                documentNumber = "X1234567<",
                primaryIdentifier = "MÜLLER",
                secondaryIdentifier = "JÖRG",
                nationality = "DEU",
                dateOfBirth = dob,
                sex = Sex.MALE,
                dateOfExpiry = doe,
                personalNumber = "",
                transliteration = IcaoDefaultTransliterationProfile,
            )
        val success = assertIs<GenerationResult.Success>(result)
        // The name field is transliterated under the no-expansion convention: Ü → U, Ö → O.
        val line1 = success.mrz[0]
        assertEquals(44, line1.length)
        assertEquals("P<DEU", line1.substring(0, 5))
        assertEquals("MULLER<<JORG", line1.substring(5).trimEnd('<'))
        val details = assertNotNull(success.metadata.transliterationDetails)
        assertEquals(IcaoDefaultTransliterationProfile.IDENTIFIER, details.profileIdentifier)
        assertEquals(2, details.transliteratedFields.size)
        assertEquals(MrzField.NAME_FIELD, details.transliteratedFields[0].field)
        assertEquals("MÜLLER", details.transliteratedFields[0].originalInput)
        assertEquals("MULLER", details.transliteratedFields[0].transliteratedOutput)
        assertEquals("JÖRG", details.transliteratedFields[1].originalInput)
        assertEquals("JORG", details.transliteratedFields[1].transliteratedOutput)
    }

    @Test
    fun td3_with_aze_profile_uses_aze_specific_schwa_mapping() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "AZE",
                documentNumber = "AB1234567",
                primaryIdentifier = "ƏLİYEV",
                secondaryIdentifier = "ƏLİ",
                nationality = "AZE",
                dateOfBirth = dob,
                sex = Sex.MALE,
                dateOfExpiry = doe,
                personalNumber = "",
                transliteration = AzeTransliterationProfile,
            )
        val success = assertIs<GenerationResult.Success>(result)
        // Schwa maps to A under AZE (where ICAO would map to E).
        val line1 = success.mrz[0]
        assertEquals(44, line1.length)
        assertEquals("P<AZE", line1.substring(0, 5))
        assertEquals("ALIYEV<<ALI", line1.substring(5).trimEnd('<'))
        val details = assertNotNull(success.metadata.transliterationDetails)
        assertEquals("AZE", details.profileIdentifier)
    }

    @Test
    fun td3_without_profile_and_non_mrz_input_returns_unsupported_characters_error() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "DEU",
                documentNumber = "X1234567<",
                primaryIdentifier = "MÜLLER",
                secondaryIdentifier = "JÖRG",
                nationality = "DEU",
                dateOfBirth = dob,
                sex = Sex.MALE,
                dateOfExpiry = doe,
                personalNumber = "",
            )
        val failure = assertIs<GenerationResult.Failure>(result)
        val error = assertIs<MrzGenerationUnsupportedCharacters>(failure.error)
        assertEquals(MrzField.NAME_FIELD, error.field)
    }

    @Test
    fun td3_with_profile_that_fails_to_map_returns_failure_with_details_in_metadata() {
        val failingProfile =
            object : io.lightine.tessera.mrz.transliteration.TransliterationProfile {
                override val identifier: String = "TEST-NEVER-MAPS"

                override fun toMrzAlphabet(normalizedInput: String): io.lightine.tessera.mrz.transliteration.TransliterationResult =
                    io.lightine.tessera.mrz.transliteration.TransliterationResult.Failure(
                        normalizedInput.mapIndexed { idx, c ->
                            io.lightine.tessera.domain.vocabulary
                                .UnmappedCharacter(c, idx)
                        },
                    )
            }
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "DEU",
                documentNumber = "X1234567<",
                primaryIdentifier = "ABC",
                secondaryIdentifier = "",
                nationality = "DEU",
                dateOfBirth = dob,
                sex = Sex.MALE,
                dateOfExpiry = doe,
                personalNumber = "",
                transliteration = failingProfile,
            )
        val failure = assertIs<GenerationResult.Failure>(result)
        val error = assertIs<MrzGenerationUnsupportedCharacters>(failure.error)
        assertEquals(MrzField.NAME_FIELD, error.field)
        assertEquals(3, error.unmappedCharacters.size)
    }

    @Test
    fun td3_mononym_input_omits_secondary_separator() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "CHER",
                secondaryIdentifier = "",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                personalNumber = "",
            )
        val success = assertIs<GenerationResult.Success>(result)
        // No `<<` separator when secondary is empty.
        val line1 = success.mrz[0]
        assertEquals(44, line1.length)
        assertEquals("P<UTOCHER", line1.trimEnd('<'))
    }

    // ---------- TD2 ----------

    @Test
    fun td2_primitive_input_succeeds_and_round_trips() {
        val result =
            MrzGenerator.generateTD2(
                documentType = "I",
                issuingState = "UTO",
                documentNumber = "D23145890",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "ANNA MARIA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                optionalData = "ZE18422",
            )
        val success = assertIs<GenerationResult.Success>(result)
        val parsed = MrzParser.parseTD2(success.mrz, referenceTime = ref2026)
        val parseSuccess = assertIs<ParseResult.Success>(parsed)
        val td2 = assertIs<TD2>(parseSuccess.document)
        assertEquals("ERIKSSON", td2.commonFields.primaryIdentifier)
        assertEquals("ANNA MARIA", td2.commonFields.secondaryIdentifier)
        assertEquals("D23145890", td2.commonFields.documentNumber)
    }

    @Test
    fun td2_with_profile_surfaces_transliteration_details() {
        val result =
            MrzGenerator.generateTD2(
                documentType = "I",
                issuingState = "DEU",
                documentNumber = "D23145890",
                primaryIdentifier = "STRAßE",
                secondaryIdentifier = "JÖRG",
                nationality = "DEU",
                dateOfBirth = dob,
                sex = Sex.MALE,
                dateOfExpiry = doe,
                optionalData = "",
                transliteration = IcaoDefaultTransliterationProfile,
            )
        val success = assertIs<GenerationResult.Success>(result)
        val details = assertNotNull(success.metadata.transliterationDetails)
        assertEquals("ICAO", details.profileIdentifier)
        // ß → SS expansion in the primary identifier.
        assertEquals("STRASSE", details.transliteratedFields[0].transliteratedOutput)
    }

    // ---------- TD1 ----------

    @Test
    fun td1_primitive_input_succeeds_and_round_trips() {
        val result =
            MrzGenerator.generateTD1(
                documentType = "I",
                issuingState = "UTO",
                documentNumber = "D23145890",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "ANNA MARIA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                optionalData1 = "1234567",
                optionalData2 = "",
            )
        val success = assertIs<GenerationResult.Success>(result)
        val parsed = MrzParser.parseTD1(success.mrz, referenceTime = ref2026)
        val parseSuccess = assertIs<ParseResult.Success>(parsed)
        val td1 = assertIs<TD1>(parseSuccess.document)
        assertEquals("ERIKSSON", td1.commonFields.primaryIdentifier)
        assertEquals("ANNA MARIA", td1.commonFields.secondaryIdentifier)
        assertEquals("1234567", td1.optionalData1.trimEnd('<'))
    }

    // ---------- MRV-A ----------

    @Test
    fun mrva_primitive_input_succeeds() {
        val result =
            MrzGenerator.generateMrvA(
                documentType = "V",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "ANNA MARIA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                optionalData = "",
            )
        val success = assertIs<GenerationResult.Success>(result)
        val parsed = MrzParser.parseMRVA(success.mrz, referenceTime = ref2026)
        val parseSuccess = assertIs<ParseResult.Success>(parsed)
        val mrvA = assertIs<MrvA>(parseSuccess.document)
        assertEquals("ERIKSSON", mrvA.commonFields.primaryIdentifier)
    }

    // ---------- MRV-B ----------

    @Test
    fun mrvb_primitive_input_succeeds() {
        val result =
            MrzGenerator.generateMrvB(
                documentType = "V",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "ANNA MARIA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                optionalData = "",
            )
        val success = assertIs<GenerationResult.Success>(result)
        val parsed = MrzParser.parseMRVB(success.mrz, referenceTime = ref2026)
        val parseSuccess = assertIs<ParseResult.Success>(parsed)
        val mrvB = assertIs<MrvB>(parseSuccess.document)
        assertEquals("ERIKSSON", mrvB.commonFields.primaryIdentifier)
    }

    // ---------- Cross-cutting ----------

    @Test
    fun unspecified_sex_encodes_as_filler_character() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.UNSPECIFIED,
                dateOfExpiry = doe,
                personalNumber = "",
            )
        val success = assertIs<GenerationResult.Success>(result)
        // Sex position in TD3 line 2 is index 20.
        assertEquals('<', success.mrz[1][20])
    }

    @Test
    fun date_extracts_last_two_digits_of_year() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "",
                nationality = "UTO",
                dateOfBirth = LocalDate(2003, 1, 15),
                sex = Sex.MALE,
                dateOfExpiry = LocalDate(2031, 12, 31),
                personalNumber = "",
            )
        val success = assertIs<GenerationResult.Success>(result)
        // DOB raw form is yymmdd = 030115; DOE raw form is 311231.
        assertEquals("030115", success.mrz[1].substring(13, 19))
        assertEquals("311231", success.mrz[1].substring(21, 27))
    }

    @Test
    fun empty_string_inputs_with_no_profile_produce_filler_only_fields() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                personalNumber = "",
            )
        val success = assertIs<GenerationResult.Success>(result)
        // Document number field is fully filler.
        assertEquals("<<<<<<<<<", success.mrz[1].substring(0, 9))
    }

    @Test
    fun digit_in_primary_identifier_returns_numeric_in_name_field_error() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "SMITH2",
                secondaryIdentifier = "ANNA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                personalNumber = "",
            )
        val failure = assertIs<GenerationResult.Failure>(result)
        val error = assertIs<MrzGenerationNumericInNameField>(failure.error)
        assertEquals(MrzFormat.TD3, error.format)
        assertEquals("SMITH2", error.observedValue)
        assertEquals(listOf('2'), error.numericCharacters)
    }

    @Test
    fun digit_in_secondary_identifier_returns_numeric_in_name_field_error() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "ERIKSSON",
                secondaryIdentifier = "ANNA M4RIA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                personalNumber = "",
            )
        val failure = assertIs<GenerationResult.Failure>(result)
        val error = assertIs<MrzGenerationNumericInNameField>(failure.error)
        assertEquals(MrzFormat.TD3, error.format)
        assertEquals("ANNA M4RIA", error.observedValue)
        assertEquals(listOf('4'), error.numericCharacters)
    }

    @Test
    fun digit_check_runs_before_profile_so_error_references_original_input() {
        val result =
            MrzGenerator.generateTD3(
                documentType = "P",
                issuingState = "UTO",
                documentNumber = "L898902C<",
                primaryIdentifier = "MÜLLER9",
                secondaryIdentifier = "ANNA",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.FEMALE,
                dateOfExpiry = doe,
                personalNumber = "",
                transliteration = IcaoDefaultTransliterationProfile,
            )
        val failure = assertIs<GenerationResult.Failure>(result)
        val error = assertIs<MrzGenerationNumericInNameField>(failure.error)
        // observedValue is the consumer's original input, not the transliterated form.
        assertEquals("MÜLLER9", error.observedValue)
        assertEquals(listOf('9'), error.numericCharacters)
    }

    @Test
    fun multiple_digits_in_name_field_are_all_reported() {
        val result =
            MrzGenerator.generateTD1(
                documentType = "I",
                issuingState = "UTO",
                documentNumber = "X1234567<",
                primaryIdentifier = "SMITH",
                secondaryIdentifier = "1A2B3C",
                nationality = "UTO",
                dateOfBirth = dob,
                sex = Sex.MALE,
                dateOfExpiry = doe,
                optionalData1 = "",
                optionalData2 = "",
            )
        val failure = assertIs<GenerationResult.Failure>(result)
        val error = assertIs<MrzGenerationNumericInNameField>(failure.error)
        assertEquals(MrzFormat.TD1, error.format)
        assertEquals(listOf('1', '2', '3'), error.numericCharacters)
    }

    @Test
    fun format_consistency_unsupported_characters_use_the_correct_format_in_error() {
        val td1Result =
            MrzGenerator.generateTD1(
                documentType = "I",
                issuingState = "DEU",
                documentNumber = "X1234567<",
                primaryIdentifier = "MÜLLER",
                secondaryIdentifier = "",
                nationality = "DEU",
                dateOfBirth = dob,
                sex = Sex.MALE,
                dateOfExpiry = doe,
                optionalData1 = "",
                optionalData2 = "",
            )
        val failure = assertIs<GenerationResult.Failure>(td1Result)
        val error = assertIs<MrzGenerationUnsupportedCharacters>(failure.error)
        assertEquals(MrzFormat.TD1, error.format)
    }
}
