package io.lightine.tessera.domain

public data class MrzCharacterSetViolation(
    val offendingCharacter: Char,
    val position: Int,
) : MrzError() {
    override val description: String
        get() = "Character '$offendingCharacter' at position $position is outside the MRZ alphabet (A-Z, 0-9, <)"
}
