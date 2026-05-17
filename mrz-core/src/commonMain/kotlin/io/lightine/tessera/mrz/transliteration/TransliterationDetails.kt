package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.domain.vocabulary.MrzField

// Per-field audit trail of transliteration applied during MRZ generation.
// Exposed on `ResultMetadata.transliterationDetails` per Principle 5 and ADR-014:
// consumers can inspect the post-normalization, pre-transliteration form for each
// field the SDK rewrote, alongside the original input and the encoded output.
public data class TransliterationDetails(
    val profileIdentifier: String,
    val transliteratedFields: List<TransliteratedField>,
)

public data class TransliteratedField(
    val field: MrzField,
    val originalInput: String,
    val normalizedInput: String,
    val transliteratedOutput: String,
)
