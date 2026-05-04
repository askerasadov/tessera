package io.lightine.tessera.mrz

import io.lightine.tessera.domain.DocumentCategory

public data class DocumentTypeCodeEntry(
    val code: String,
    val displayName: String,
    val category: DocumentCategory,
    val generation: DocumentTypeGeneration,
)
