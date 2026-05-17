package io.lightine.tessera.domain.errors

public data class MrzNameTruncated(
    val rawNameField: String,
    val position: Int,
) : MrzWarning() {
    override val description: String
        get() = "Name field at position $position fills the available width with no trailing filler, indicating ICAO Doc 9303 truncation"
}
