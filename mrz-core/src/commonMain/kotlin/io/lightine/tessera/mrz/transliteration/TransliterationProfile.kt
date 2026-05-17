package io.lightine.tessera.mrz.transliteration

public interface TransliterationProfile {
    public val identifier: String

    /** Applies this profile's mapping rules. [normalizedInput] is assumed Unicode NFC (per ADR-014). */
    public fun toMrzAlphabet(normalizedInput: String): TransliterationResult
}
