package io.lightine.tessera.mrz

import io.lightine.tessera.domain.DocumentCategory
import kotlin.jvm.JvmInline

@JvmInline
public value class DocumentType(
    public val rawCode: String,
) {
    public val entry: DocumentTypeCodeEntry?
        get() = DocumentTypeCodeTable.lookup(rawCode)

    public val isRecognized: Boolean
        get() = entry != null

    public val category: DocumentCategory?
        get() = entry?.category
}
