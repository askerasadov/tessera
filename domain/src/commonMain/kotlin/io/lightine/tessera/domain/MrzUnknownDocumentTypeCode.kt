package io.lightine.tessera.domain

public data class MrzUnknownDocumentTypeCode(
    val rawCode: String,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() = "Document type code '$rawCode' at position $position is not in the SDK's recognized lookup tables"
}
