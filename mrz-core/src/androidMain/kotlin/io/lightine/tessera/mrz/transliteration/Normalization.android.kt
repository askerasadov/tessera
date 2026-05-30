package io.lightine.tessera.mrz.transliteration

import java.text.Normalizer

// Mirrors the JVM actual (Normalization.jvm.kt): Android exposes the same java.text.Normalizer
// (available since API 1), so the implementation is identical. Kept as a separate androidMain
// source file rather than a shared source set because there is a single one-line actual; if more
// JVM/Android-shared code appears, factor out an intermediate source set then.
internal actual fun normalizeForTransliteration(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFC)
