package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzGenerationNumericInNameFieldTest {
    @Test
    fun stores_format_observed_value_and_numeric_characters_verbatim() {
        val error =
            MrzGenerationNumericInNameField(
                format = MrzFormat.TD3,
                observedValue = "SMITH2",
                numericCharacters = listOf('2'),
            )
        assertEquals(MrzFormat.TD3, error.format)
        assertEquals("SMITH2", error.observedValue)
        assertEquals(listOf('2'), error.numericCharacters)
    }

    @Test
    fun description_names_format_digits_and_observed_value() {
        val error =
            MrzGenerationNumericInNameField(
                format = MrzFormat.TD3,
                observedValue = "ANNA M4RIA",
                numericCharacters = listOf('4'),
            )
        assertTrue("TD3" in error.description)
        assertTrue("'4'" in error.description)
        assertTrue("ANNA M4RIA" in error.description)
        assertTrue("§4.6" in error.description || "4.6" in error.description)
    }

    @Test
    fun description_lists_distinct_digits_when_multiple_present() {
        val error =
            MrzGenerationNumericInNameField(
                format = MrzFormat.TD1,
                observedValue = "1A2B1",
                numericCharacters = listOf('1', '2', '1'),
            )
        // Distinct digits — '1' should appear once, not twice.
        val firstQuoteAtOne = error.description.indexOf("'1'")
        val secondQuoteAtOne = error.description.indexOf("'1'", firstQuoteAtOne + 1)
        assertTrue(secondQuoteAtOne == -1, "Description should list distinct digits; got '${error.description}'")
        assertTrue("'2'" in error.description)
    }

    @Test
    fun is_a_generation_error_and_an_mrz_error() {
        val error: MrzError = MrzGenerationNumericInNameField(MrzFormat.TD3, "X1", listOf('1'))
        assertTrue(error is MrzGenerationError)
    }
}
