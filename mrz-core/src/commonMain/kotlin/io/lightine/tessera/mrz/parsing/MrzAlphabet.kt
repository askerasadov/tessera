package io.lightine.tessera.mrz.parsing

/**
 * True if [c] is in the MRZ alphabet — uppercase `A`–`Z`, digits `0`–`9`, or the filler
 * character `<`. This is the complete set of characters ICAO Doc 9303 permits anywhere
 * in a Machine Readable Zone.
 */
public fun isMrzAlphabetCharacter(c: Char): Boolean = c in '0'..'9' || c in 'A'..'Z' || c == '<'
