package io.lightine.tessera.mrz

import io.lightine.tessera.domain.DocumentCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentTypeTest {
    @Test
    fun raw_code_is_preserved_verbatim_for_recognized_code() {
        assertEquals("P", DocumentType("P").rawCode)
    }

    @Test
    fun raw_code_is_preserved_verbatim_for_unrecognized_code() {
        assertEquals("XX", DocumentType("XX").rawCode)
    }

    @Test
    fun is_recognized_is_true_when_code_is_in_lookup_table() {
        assertTrue(DocumentType("P").isRecognized)
        assertTrue(DocumentType("PD").isRecognized)
    }

    @Test
    fun is_recognized_is_false_when_code_is_not_in_lookup_table() {
        assertFalse(DocumentType("XX").isRecognized)
    }

    @Test
    fun category_resolves_via_lookup_table_for_recognized_code() {
        assertEquals(DocumentCategory.PASSPORT, DocumentType("P").category)
        assertEquals(DocumentCategory.VISA, DocumentType("V").category)
    }

    @Test
    fun category_is_null_for_unrecognized_code() {
        assertNull(DocumentType("XX").category)
    }

    @Test
    fun entry_resolves_via_lookup_table_for_recognized_code() {
        val entry = DocumentType("PD").entry
        assertNotNull(entry)
        assertEquals("Diplomatic passport", entry.displayName)
        assertEquals(DocumentTypeGeneration.CURRENT_TWO_CHARACTER, entry.generation)
    }

    @Test
    fun entry_is_null_for_unrecognized_code() {
        assertNull(DocumentType("XX").entry)
    }

    @Test
    fun two_document_types_with_same_raw_code_are_equal() {
        assertEquals(DocumentType("P"), DocumentType("P"))
    }

    @Test
    fun preserves_empty_raw_code_verbatim_without_recognizing_it() {
        val empty = DocumentType("")
        assertEquals("", empty.rawCode)
        assertFalse(empty.isRecognized)
    }
}
