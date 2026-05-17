package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField

public data class MrzCheckDigitMismatch(
    val field: MrzField,
    val expected: Char,
    val observed: Char,
    val position: Int,
) : MrzValidationError() {
    override val description: String
        get() = "Check digit for ${this.field} at position $position: expected '$expected', observed '$observed'"
}
