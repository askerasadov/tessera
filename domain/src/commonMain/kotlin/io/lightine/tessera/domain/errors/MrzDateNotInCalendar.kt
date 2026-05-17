package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField

/**
 * Validation failure: a date field's raw components (year/month/day) do not form a real
 * calendar date for any candidate century (e.g., month `13`, day `32`, or `February 30`).
 * Carries the raw components verbatim so consumers can render them as the document
 * presents them.
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator] when
 * [`MrzDate.componentsFormCalendarDate`][io.lightine.tessera.mrz.model.MrzDate.componentsFormCalendarDate]
 * is `false`. Distinct from inference-window misses (e.g., expiry > 50 years out): those
 * dates are in the calendar but outside the SDK's plausibility window, and do not emit
 * this failure.
 */
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
