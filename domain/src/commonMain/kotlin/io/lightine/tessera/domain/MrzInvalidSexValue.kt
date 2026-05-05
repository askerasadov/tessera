package io.lightine.tessera.domain

public data class MrzInvalidSexValue(
    val observed: Char,
    val position: Int,
) : MrzValidationError() {
    override val description: String
        get() = "Sex field at position $position contains '$observed'; allowed values are 'M', 'F', '<', or 'X'"
}
