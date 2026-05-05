package io.lightine.tessera.domain

public data class MrzCheckDigitMismatch(
    val field: MrzField,
    val expected: Char,
    val observed: Char,
    val position: Int,
) : MrzValidationError() {
    override val description: String
        get() = "Check digit for ${this.field} at position $position: expected '$expected', observed '$observed'"
}
