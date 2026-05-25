package io.lightine.tessera.types.errors

import kotlinx.datetime.LocalDate

/**
 * Warning: every century candidate the SDK could pick for the birth date implies an age
 * greater than [thresholdYears] relative to [referenceDate]. Carries the raw date
 * components verbatim so consumers can render them as the document presents them.
 *
 * Emitted by [`MrzValidator`][io.lightine.tessera.mrz.validation.MrzValidator] when the
 * birth date's century could not be inferred (because both 1900 and 2000-base years would
 * imply ages > [thresholdYears]). The threshold is currently 130 years — outside the
 * known range of human lifespans, but inside the range of recoverable transcription errors.
 *
 * A warning rather than a validation failure because the SDK does not commit to "this
 * document is invalid" based on age plausibility alone (Principle 1 — reader, not oracle).
 */
public data class MrzBirthDateImplausiblyOld(
    val rawYear: String,
    val rawMonth: String,
    val rawDay: String,
    val referenceDate: LocalDate,
    val thresholdYears: Int,
) : MrzWarning() {
    override val description: String
        get() =
            "Document birth date components (year='$rawYear', month='$rawMonth', day='$rawDay') " +
                "imply an age greater than $thresholdYears years at every candidate century " +
                "relative to reference date $referenceDate"
}
