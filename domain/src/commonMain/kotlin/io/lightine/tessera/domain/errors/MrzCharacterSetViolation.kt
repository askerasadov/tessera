package io.lightine.tessera.domain.errors

public data class MrzCharacterSetViolation(
    val offendingCharacter: Char,
    val position: Int,
) : MrzParseError() {
    override val description: String
        get() = "Character '$offendingCharacter' at position $position is outside the MRZ alphabet (A-Z, 0-9, <)"
}
