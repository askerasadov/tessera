package io.lightine.tessera.types.errors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzUnknownDocumentTypeCodeTest {
    @Test
    fun stores_raw_code_and_position_verbatim() {
        val warning = MrzUnknownDocumentTypeCode(rawCode = "XY", position = 0)
        assertEquals("XY", warning.rawCode)
        assertEquals(0, warning.position)
    }

    @Test
    fun preserves_empty_raw_code_verbatim() {
        val warning = MrzUnknownDocumentTypeCode(rawCode = "", position = 0)
        assertEquals("", warning.rawCode)
    }

    @Test
    fun description_names_raw_code_and_position() {
        val warning = MrzUnknownDocumentTypeCode(rawCode = "XY", position = 0)
        val description = warning.description
        assertTrue("XY" in description, "Description should mention rawCode; got '$description'")
        assertTrue("0" in description, "Description should mention position; got '$description'")
    }

    @Test
    fun is_a_warning() {
        val warning: MrzWarning = MrzUnknownDocumentTypeCode(rawCode = "XY", position = 0)
        assertTrue(warning is MrzUnknownDocumentTypeCode)
    }
}
