package io.lightine.tessera.domain

public data class MrzDateNotInCalendar(
    val field: MrzField,
    val rawYear: String,
    val rawMonth: String,
    val rawDay: String,
    val position: Int,
) : MrzValidationError() {
    override val description: String
        get() =
            "Date in ${this.field} at position $position is not a real calendar date: " +
                "rawYear='$rawYear', rawMonth='$rawMonth', rawDay='$rawDay'"
}
