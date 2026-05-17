package io.lightine.tessera.mrz.transliteration

/** Normalizes input to Unicode NFC before profile lookup. Per ADR-014. */
internal expect fun normalizeForTransliteration(input: String): String
