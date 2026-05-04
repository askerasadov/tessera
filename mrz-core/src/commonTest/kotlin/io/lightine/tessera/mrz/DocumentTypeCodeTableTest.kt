package io.lightine.tessera.mrz

import io.lightine.tessera.domain.DocumentCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentTypeCodeTableTest {
    @Test
    fun lookup_returns_entry_for_legacy_passport_code_p() {
        val entry = DocumentTypeCodeTable.lookup("P")
        assertNotNull(entry)
        assertEquals("Passport", entry.displayName)
        assertEquals(DocumentCategory.PASSPORT, entry.category)
        assertEquals(DocumentTypeGeneration.LEGACY_SINGLE_CHARACTER, entry.generation)
    }

    @Test
    fun lookup_returns_entry_for_current_diplomatic_passport_code_pd() {
        val entry = DocumentTypeCodeTable.lookup("PD")
        assertNotNull(entry)
        assertEquals(DocumentCategory.PASSPORT, entry.category)
        assertEquals(DocumentTypeGeneration.CURRENT_TWO_CHARACTER, entry.generation)
    }

    @Test
    fun lookup_returns_null_for_unrecognized_code() {
        assertNull(DocumentTypeCodeTable.lookup("XX"))
    }

    @Test
    fun lookup_is_case_sensitive_per_icao_alphabet() {
        // MRZ alphabet is uppercase only; lowercase 'p' is not a valid MRZ document type code.
        assertNull(DocumentTypeCodeTable.lookup("p"))
    }

    @Test
    fun all_returns_every_entry_in_the_starter_set() {
        val codes = DocumentTypeCodeTable.all().map { it.code }.toSet()
        assertEquals(setOf("P", "V", "I", "PP", "PD", "PS"), codes)
    }

    @Test
    fun by_category_passport_returns_all_passport_entries() {
        val passportCodes = DocumentTypeCodeTable.byCategory(DocumentCategory.PASSPORT).map { it.code }.toSet()
        assertEquals(setOf("P", "PP", "PD", "PS"), passportCodes)
    }

    @Test
    fun by_category_visa_returns_only_visa_entries() {
        val visaEntries = DocumentTypeCodeTable.byCategory(DocumentCategory.VISA)
        assertEquals(1, visaEntries.size)
        assertEquals("V", visaEntries.single().code)
    }

    @Test
    fun by_category_identity_card_returns_only_identity_card_entries() {
        val idEntries = DocumentTypeCodeTable.byCategory(DocumentCategory.IDENTITY_CARD)
        assertEquals(1, idEntries.size)
        assertEquals("I", idEntries.single().code)
    }

    @Test
    fun by_category_residence_permit_returns_empty_list_in_starter_set() {
        // Starter set has no residence permit codes; this lock surfaces explicitly when the set is expanded.
        assertTrue(DocumentTypeCodeTable.byCategory(DocumentCategory.RESIDENCE_PERMIT).isEmpty())
    }
}
