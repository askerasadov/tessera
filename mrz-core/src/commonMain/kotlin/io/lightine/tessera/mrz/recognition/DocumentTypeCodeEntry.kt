package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.types.vocabulary.DocumentCategory

/**
 * A single entry in [DocumentTypeCodeTable]: the raw document type code as it appears in
 * an MRZ, the SDK's display name for it, the high-level category it falls under, and
 * which generation of the document type code convention it belongs to.
 *
 * Returned by [DocumentTypeCodeTable.lookup] and indirectly via [DocumentType.entry].
 */
public data class DocumentTypeCodeEntry(
    val code: String,
    val displayName: String,
    val category: DocumentCategory,
    val generation: DocumentTypeGeneration,
)
