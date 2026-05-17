package io.lightine.tessera.mrz.transliteration

// Shared internal builder for the Latin-script mapping table used by profiles
// whose conventions align with ICAO Doc 9303 Part 3 Section 6 (Annex G).
//
// This is implementation sharing, not profile inheritance: the public API has
// no inheritance mechanism (see docs/open-questions.md, "Profile inheritance
// for transliteration"). Profiles that want to diverge from these defaults
// build a copy and apply their own overrides; profiles whose conventions are
// not Latin-aligned do not use this helper at all.
internal fun buildIcaoLatinMappings(): MutableMap<Char, String> {
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
