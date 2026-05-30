package io.lightine.tessera.mrz.transliteration

import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCanonicalMapping

// iOS has no java.text.Normalizer. Foundation's NSString.precomposedStringWithCanonicalMapping is the
// canonical NFC normalization (the NFD counterpart is decomposedStringWithCanonicalMapping). It lives
// in the shared iosMain source set so all three iOS targets (arm64, simulator-arm64, x64) use it.
// Per ADR-014.
//
// CAST_NEVER_SUCCEEDS is a known false positive here: Kotlin/Native bridges kotlin.String to NSString
// at runtime, but the compiler's static cast analysis treats the two as unrelated and flags the cast.
// The String<->NSString bridge is a documented, stable Kotlin/Native feature; the cast succeeds at
// runtime (verified by running NormalizationTest on the iOS simulator).
@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun normalizeForTransliteration(input: String): String = (input as NSString).precomposedStringWithCanonicalMapping
