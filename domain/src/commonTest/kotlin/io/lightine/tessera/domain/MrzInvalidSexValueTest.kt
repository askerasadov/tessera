package io.lightine.tessera.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzInvalidSexValueTest {
    @Test
    fun stores_observed_character_and_position_verbatim() {
        val error =
            MrzInvalidSexValue(
                observed = 'Q',
                position = 64,
            )
        assertEquals('Q', error.observed)
        assertEquals(64, error.position)
    }

    @Test
    fun description_names_observed_character_and_position() {
        val error = MrzInvalidSexValue(observed = '*', position = 64)
        val description = error.description
        assertTrue("'*'" in description, "Description should mention observed character; got '$description'")
        assertTrue("64" in description, "Description should mention position; got '$description'")
    }

    @Test
    fun is_a_validation_error() {
        val error: MrzValidationError = MrzInvalidSexValue(observed = '?', position = 0)
        assertTrue(error is MrzInvalidSexValue)
    }
}
