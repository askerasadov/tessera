package io.lightine.tessera.types.errors

/**
 * Warning: the document's [rawCode] is not present in
 * [`DocumentTypeCodeTable`][io.lightine.tessera.mrz.recognition.DocumentTypeCodeTable].
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator]. The SDK's
 * document type code table is deliberately a starter set — see
 * [`docs/features/lookup-tables.md`](https://github.com/lightine-io/tessera/blob/main/docs/features/lookup-tables.md);
 * tracked in `docs/open-questions.md`. A warning rather than a validation failure per
 * [ADR-013](https://github.com/lightine-io/tessera/blob/main/docs/decisions/0013-recognition-failures-are-warnings.md):
 * "this code isn't in our table" is not the same as "this code is invalid."
 */
public data class MrzUnknownDocumentTypeCode(
    val rawCode: String,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() = "Document type code '$rawCode' at position $position is not in the SDK's recognized lookup tables"
}
