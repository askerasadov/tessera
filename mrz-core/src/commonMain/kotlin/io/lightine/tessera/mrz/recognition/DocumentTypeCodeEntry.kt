package io.lightine.tessera.mrz.recognition

import io.lightine.tessera.domain.vocabulary.DocumentCategory

public data class DocumentTypeCodeEntry(
    val code: String,
    val displayName: String,
    val category: DocumentCategory,
    val generation: DocumentTypeGeneration,
)
