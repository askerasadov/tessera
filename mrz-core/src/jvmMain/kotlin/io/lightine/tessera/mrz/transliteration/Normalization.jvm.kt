package io.lightine.tessera.mrz.transliteration

import java.text.Normalizer

internal actual fun normalizeForTransliteration(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFC)
