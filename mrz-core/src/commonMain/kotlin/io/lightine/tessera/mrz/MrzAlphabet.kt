package io.lightine.tessera.mrz

public fun isMrzAlphabetCharacter(c: Char): Boolean = c in '0'..'9' || c in 'A'..'Z' || c == '<'
