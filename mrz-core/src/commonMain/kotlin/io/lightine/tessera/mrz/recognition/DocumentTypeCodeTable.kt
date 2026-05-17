package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.domain.vocabulary.DocumentCategory

/**
 * The SDK's recognized document type codes for MRZ document type fields.
 *
 * **Deliberate starter set.** This table is intentionally incomplete relative to the
 * canonical source (ICAO Doc 9303 Part 3 Section 4). Adding entries is a non-breaking
 * change. See
 * [`docs/features/lookup-tables.md`](https://github.com/askerasadov/Tessera/blob/main/docs/features/lookup-tables.md)
 * for the design and `docs/open-questions.md` for tracking ("Document type code table
 * completeness").
 *
 * Codes not present in this table surface as
 * [`MrzUnknownDocumentTypeCode`][io.lightine.tessera.domain.errors.MrzUnknownDocumentTypeCode]
 * warnings rather than validation failures, per
 * [ADR-013](https://github.com/askerasadov/Tessera/blob/main/docs/decisions/0013-recognition-failures-are-warnings.md).
 */
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

    /** Returns the entry for [code], or `null` if the code is not in the table. */
    public fun lookup(code: String): DocumentTypeCodeEntry? = byCode[code]

    /** Returns every entry currently in the table, in registration order. */
    public fun all(): List<DocumentTypeCodeEntry> = entries

    /** Returns every entry in the table whose category matches [category]. */
    public fun byCategory(category: DocumentCategory): List<DocumentTypeCodeEntry> = entries.filter { it.category == category }
}
