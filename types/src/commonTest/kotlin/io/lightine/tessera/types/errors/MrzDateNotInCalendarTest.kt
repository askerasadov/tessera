package io.lightine.tessera.types.errors

import io.lightine.tessera.types.vocabulary.MrzField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzDateNotInCalendarTest {
    @Test
    fun stores_field_raw_components_and_position_verbatim() {
        val error =
            MrzDateNotInCalendar(
                field = MrzField.DATE_OF_BIRTH,
                rawYear = "90",
                rawMonth = "02",
                rawDay = "30",
                position = 57,
            )
        assertEquals(MrzField.DATE_OF_BIRTH, error.field)
        assertEquals("90", error.rawYear)
        assertEquals("02", error.rawMonth)
        assertEquals("30", error.rawDay)
        assertEquals(57, error.position)
    }

    @Test
    fun description_mentions_field_position_and_raw_components() {
        val error =
            MrzDateNotInCalendar(
                field = MrzField.DATE_OF_EXPIRY,
                rawYear = "30",
                rawMonth = "13",
                rawDay = "01",
                position = 65,
            )
        val description = error.description
        assertTrue("DATE_OF_EXPIRY" in description, "Description should name the field; got '$description'")
        assertTrue("65" in description, "Description should mention position; got '$description'")
        assertTrue("'30'" in description, "Description should quote rawYear; got '$description'")
        assertTrue("'13'" in description, "Description should quote rawMonth; got '$description'")
        assertTrue("'01'" in description, "Description should quote rawDay; got '$description'")
    }

    @Test
    fun is_a_validation_error() {
        val error: MrzValidationError =
            MrzDateNotInCalendar(
                field = MrzField.DATE_OF_BIRTH,
                rawYear = "90",
                rawMonth = "02",
                rawDay = "30",
                position = 57,
            )
        assertTrue(error is MrzDateNotInCalendar)
    }
}
