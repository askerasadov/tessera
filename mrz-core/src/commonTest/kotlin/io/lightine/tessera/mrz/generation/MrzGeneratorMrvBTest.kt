package io.lightine.tessera.mrz.generation

import io.lightine.tessera.domain.errors.MrzGenerationFieldOverflow
import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat
import io.lightine.tessera.mrz.model.MrvB
import io.lightine.tessera.mrz.parsing.MrzParser
import io.lightine.tessera.mrz.parsing.ParseResult
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MrzGeneratorMrvBTest {
    private val ref2026 = Instant.parse("2026-05-04T12:00:00Z")

    private val specimenLine1 = "V<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<"
    private val specimenLine2 = "L898902C<3UTO6908061F3008063<<<<<<<<"
    private val specimenLines = listOf(specimenLine1, specimenLine2)

    private fun specimenMrvB(): MrvB {
        val result = MrzParser.parseMRVB(specimenLines, referenceTime = ref2026)
        return assertIs<MrvB>(assertIs<ParseResult.Success>(result).document)
    }

    @Test
    fun generate_produces_specimen_lines_verbatim() {
        val regenerated = assertIs<GenerationResult.Success>(MrzGenerator.generate(specimenMrvB()))
        assertEquals(specimenLines, regenerated.mrz)
    }

    @Test
    fun generate_then_parse_round_trips_raw_fields() {
        val mrvB = specimenMrvB()
        val regenerated = assertIs<GenerationResult.Success>(MrzGenerator.generate(mrvB))
        val reparsed = assertIs<ParseResult.Success>(MrzParser.parseMRVB(regenerated.mrz, referenceTime = ref2026))
        val mrvBRoundTripped = assertIs<MrvB>(reparsed.document)

        assertEquals(mrvB.commonFields.documentNumber, mrvBRoundTripped.commonFields.documentNumber)
        assertEquals(mrvB.commonFields.rawSex, mrvBRoundTripped.commonFields.rawSex)
        assertEquals(mrvB.optionalData, mrvBRoundTripped.optionalData)
    }

    @Test
    fun generated_lines_have_correct_dimensions() {
        val success = assertIs<GenerationResult.Success>(MrzGenerator.generate(specimenMrvB()))
        assertEquals(2, success.mrz.size)
        success.mrz.forEach { assertEquals(36, it.length) }
    }

    @Test
    fun line_2_ends_with_optional_data_not_a_composite_check_digit() {
        // MRV-B mirrors MRV-A: no composite digit. Optional data fills positions 28-35 (8 chars).
        val success = assertIs<GenerationResult.Success>(MrzGenerator.generate(specimenMrvB()))
        assertEquals("<<<<<<<<", success.mrz[1].substring(28, 36))
    }

    @Test
    fun fails_with_overflow_when_optional_data_exceeds_eight_characters() {
        val mrvB = specimenMrvB().copy(optionalData = "X".repeat(9))
        val failure = assertIs<GenerationResult.Failure>(MrzGenerator.generate(mrvB))
        val error = assertIs<MrzGenerationFieldOverflow>(failure.error)
        assertEquals(MrzFormat.MRV_B, error.format)
        assertEquals(MrzField.OPTIONAL_DATA, error.field)
        assertEquals(8, error.maxLength)
    }
}
