package io.lightine.tessera.domain.vocabulary

/**
 * Coarse classification of a document, derived from its MRZ document type code via the
 * [`DocumentTypeCodeTable`][io.lightine.tessera.mrz.recognition.DocumentTypeCodeTable]
 * lookup. Exposed on
 * [`DocumentType.category`][io.lightine.tessera.mrz.recognition.DocumentType.category].
 *
 * Returns `null` from [`DocumentType.category`][io.lightine.tessera.mrz.recognition.DocumentType.category]
 * when the document's raw type code is not in the SDK's lookup table — a deliberate
 * starter set, not an exhaustive enumeration of every code in use worldwide. See
 * [OTHER] for documents whose code is recognized but whose category is none of the
 * specific ones below.
 */
public enum class DocumentCategory {
    PASSPORT,
    IDENTITY_CARD,
    RESIDENCE_PERMIT,
    VISA,

    /**
     * The document type is recognized in the lookup table but does not fit any of the
     * other categories.
     */
    OTHER,
}
