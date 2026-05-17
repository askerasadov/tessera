package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.mrz.parsing.isMrzAlphabetCharacter

// Default transliteration profile, implementing the recommendations in
// ICAO Doc 9303 Part 3 Section 6 (Annex G) under the no-expansion convention
// (e.g., Ä → A, ü → U). Multi-character transliterations follow the standard:
// Æ → AE, Œ → OE, ß → SS, Þ → TH, Ð → D, Ĳ → IJ.
//
// Unmapped non-MRZ characters fall back to the filler `<`, so this profile
// always returns Success.
//
// IMPORTANT: This is a deliberate starter set, not an exhaustive enumeration.
// Tracked in docs/open-questions.md under "ICAO default profile coverage
// completeness". Adding entries is a non-breaking change.
public object IcaoDefaultTransliterationProfile : TransliterationProfile {
    public const val IDENTIFIER: String = "ICAO"

    public override val identifier: String = IDENTIFIER

    private const val FILLER: Char = '<'

    private val mappings: Map<Char, String> = buildMappings()

    public override fun toMrzAlphabet(normalizedInput: String): TransliterationResult {
        val output = StringBuilder(normalizedInput.length)
        for (char in normalizedInput) {
            when {
                isMrzAlphabetCharacter(char) -> output.append(char)
                else -> output.append(mappings[char] ?: FILLER.toString())
            }
        }
        return TransliterationResult.Success(output.toString())
    }

    private fun buildMappings(): Map<Char, String> {
        val map = mutableMapOf<Char, String>()

        for (lower in 'a'..'z') map[lower] = lower.uppercaseChar().toString()

        addAll(map, "ÀÁÂÃÄÅĀĂĄàáâãäåāăą", "A")
        addAll(map, "ÇĆĈĊČçćĉċč", "C")
        addAll(map, "ĎĐďđ", "D")
        addAll(map, "ÈÉÊËĒĔĖĘĚèéêëēĕėęěƏə", "E")
        addAll(map, "ĜĞĠĢĝğġģ", "G")
        addAll(map, "ĤĦĥħ", "H")
        addAll(map, "ÌÍÎÏĨĪĬĮİìíîïĩīĭįı", "I")
        addAll(map, "Ĵĵ", "J")
        addAll(map, "Ķķ", "K")
        addAll(map, "ĹĻĽĿŁĺļľŀł", "L")
        addAll(map, "ÑŃŅŇñńņň", "N")
        addAll(map, "ÒÓÔÕÖØŌŎŐòóôõöøōŏő", "O")
        addAll(map, "ŔŖŘŕŗř", "R")
        addAll(map, "ŚŜŞŠśŝşš", "S")
        addAll(map, "ŢŤŦţťŧ", "T")
        addAll(map, "ÙÚÛÜŨŪŬŮŰŲùúûüũūŭůűų", "U")
        addAll(map, "Ŵŵ", "W")
        addAll(map, "ÝŶŸýÿŷ", "Y")
        addAll(map, "ŹŻŽźżž", "Z")

        addAll(map, "Ææ", "AE")
        addAll(map, "Œœ", "OE")
        addAll(map, "ß", "SS")
        addAll(map, "Þþ", "TH")
        addAll(map, "Ðð", "D")
        addAll(map, "Ĳĳ", "IJ")

        return map
    }

    private fun addAll(
        map: MutableMap<Char, String>,
        chars: String,
        target: String,
    ) {
        for (c in chars) map[c] = target
    }
}
