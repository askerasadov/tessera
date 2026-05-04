package io.lightine.tessera.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzInvalidLengthTest {
    @Test
    fun stores_format_and_expected_and_observed_lengths_verbatim() {
        val error =
            MrzInvalidLength(
                format = MrzFormat.TD3,
                expectedLineCount = 2,
                expectedLineLength = 44,
                observedLineCount = 1,
                observedLineLengths = listOf(44),
            )
        assertEquals(MrzFormat.TD3, error.format)
        assertEquals(2, error.expectedLineCount)
        assertEquals(44, error.expectedLineLength)
        assertEquals(1, error.observedLineCount)
        assertEquals(listOf(44), error.observedLineLengths)
    }

    @Test
    fun description_names_format_expected_and_observed() {
        val error =
            MrzInvalidLength(
                format = MrzFormat.TD3,
                expectedLineCount = 2,
                expectedLineLength = 44,
                observedLineCount = 1,
                observedLineLengths = listOf(40),
            )
        assertTrue("TD3" in error.description, "Description should name the format; got '${error.description}'")
        assertTrue(
            "2" in error.description && "44" in error.description,
            "Description should mention expected dimensions; got '${error.description}'",
        )
        assertTrue("[40]" in error.description, "Description should mention observed line lengths; got '${error.description}'")
    }

    @Test
    fun is_a_parse_error_and_an_mrz_error() {
        val error: MrzError = MrzInvalidLength(MrzFormat.TD3, 2, 44, 0, emptyList())
        assertTrue(error is MrzParseError)
        assertTrue(error is MrzInvalidLength)
    }
}
