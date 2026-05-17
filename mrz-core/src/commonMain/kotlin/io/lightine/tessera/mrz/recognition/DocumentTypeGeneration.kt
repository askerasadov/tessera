package io.lightine.tessera.mrz.recognition

/**
 * Which generation of ICAO Doc 9303 document type code conventions an entry belongs to.
 *
 * The older single-character codes (`P`, `V`, `I`, etc.) are still in widespread use; the
 * newer two-character codes (`PP`, `PD`, `PS`, etc.) are recommended by ICAO Doc 9303
 * Part 3 Section 4 for new issuances. Both generations coexist in the SDK's lookup table
 * and are emitted by real-world documents.
 */
public enum class DocumentTypeGeneration {
    LEGACY_SINGLE_CHARACTER,
    CURRENT_TWO_CHARACTER,
}
