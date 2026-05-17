package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.domain.vocabulary.DocumentCategory

// IMPORTANT: This is a deliberate starter set, not the complete enumeration
// committed to in docs/features/lookup-tables.md. The canonical source is
// ICAO Doc 9303 Part 3 Section 4. Adding entries is a non-breaking change
// (see docs/features/lookup-tables.md). Tracked in docs/open-questions.md
// under "Document type code table completeness".
public object DocumentTypeCodeTable {
    private val entries: List<DocumentTypeCodeEntry> =
        listOf(
            DocumentTypeCodeEntry(
                code = "P",
                displayName = "Passport",
                category = DocumentCategory.PASSPORT,
                generation = DocumentTypeGeneration.LEGACY_SINGLE_CHARACTER,
            ),
            DocumentTypeCodeEntry(
                code = "V",
                displayName = "Visa",
                category = DocumentCategory.VISA,
                generation = DocumentTypeGeneration.LEGACY_SINGLE_CHARACTER,
            ),
            DocumentTypeCodeEntry(
                code = "I",
                displayName = "Identity card",
                category = DocumentCategory.IDENTITY_CARD,
                generation = DocumentTypeGeneration.LEGACY_SINGLE_CHARACTER,
            ),
            DocumentTypeCodeEntry(
                code = "PP",
                displayName = "Ordinary passport",
                category = DocumentCategory.PASSPORT,
                generation = DocumentTypeGeneration.CURRENT_TWO_CHARACTER,
            ),
            DocumentTypeCodeEntry(
                code = "PD",
                displayName = "Diplomatic passport",
                category = DocumentCategory.PASSPORT,
                generation = DocumentTypeGeneration.CURRENT_TWO_CHARACTER,
            ),
            DocumentTypeCodeEntry(
                code = "PS",
                displayName = "Service passport",
                category = DocumentCategory.PASSPORT,
                generation = DocumentTypeGeneration.CURRENT_TWO_CHARACTER,
            ),
        )

    private val byCode: Map<String, DocumentTypeCodeEntry> = entries.associateBy { it.code }

    public fun lookup(code: String): DocumentTypeCodeEntry? = byCode[code]

    public fun all(): List<DocumentTypeCodeEntry> = entries

    public fun byCategory(category: DocumentCategory): List<DocumentTypeCodeEntry> = entries.filter { it.category == category }
}
