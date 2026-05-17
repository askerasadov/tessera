package io.lightine.tessera.mrz.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

public data class MrzDate(
    val rawYear: String,
    val rawMonth: String,
    val rawDay: String,
    val computedYear: Int? = null,
    val computedDate: LocalDate? = null,
    val inferenceMethod: MrzDateInferenceMethod = MrzDateInferenceMethod.RAW_ONLY,
    val componentsFormCalendarDate: Boolean? = null,
    val componentsExceedBirthAgeLimit: Boolean? = null,
) {
    public companion object {
        internal const val MAX_PLAUSIBLE_AGE_YEARS: Int = 130
        private const val EXPIRY_PAST_WINDOW_YEARS = 10
        private const val EXPIRY_FUTURE_WINDOW_YEARS = 50

        public fun parseBirth(
            rawYear: String,
            rawMonth: String,
            rawDay: String,
            referenceTime: Instant = Clock.System.now(),
        ): MrzDate {
            val parsed =
                parseRawComponents(rawYear, rawMonth, rawDay)
                    ?: return rawOnly(
                        rawYear,
                        rawMonth,
                        rawDay,
                        componentsFormCalendarDate = null,
                        componentsExceedBirthAgeLimit = null,
                    )
            val componentsFormCalendar = anyCenturyFormsCalendarDate(parsed)
            val refDate = referenceTime.toLocalDateTime(TimeZone.UTC).date
            val pickedYear =
                pickBirthYear(parsed, centuryBase = 2000, ref = refDate)
                    ?: pickBirthYear(parsed, centuryBase = 1900, ref = refDate)
                    ?: return rawOnly(
                        rawYear,
                        rawMonth,
                        rawDay,
                        componentsFormCalendarDate = componentsFormCalendar,
                        componentsExceedBirthAgeLimit =
                            if (componentsFormCalendar) {
                                anyBirthCandidateInPast(parsed, refDate)
                            } else {
                                null
                            },
                    )
            return MrzDate(
                rawYear = rawYear,
                rawMonth = rawMonth,
                rawDay = rawDay,
                computedYear = pickedYear,
                computedDate = LocalDate(pickedYear, parsed.month, parsed.day),
                inferenceMethod = MrzDateInferenceMethod.SLIDING_WINDOW_BIRTH,
                componentsFormCalendarDate = true,
                componentsExceedBirthAgeLimit = false,
            )
        }

        public fun parseExpiry(
            rawYear: String,
            rawMonth: String,
            rawDay: String,
            referenceTime: Instant = Clock.System.now(),
        ): MrzDate {
            val parsed =
                parseRawComponents(rawYear, rawMonth, rawDay)
                    ?: return rawOnly(rawYear, rawMonth, rawDay, componentsFormCalendarDate = null)
            val componentsFormCalendar = anyCenturyFormsCalendarDate(parsed)
            val refDate = referenceTime.toLocalDateTime(TimeZone.UTC).date
            val pickedYear =
                pickExpiryYear(parsed, centuryBase = 2000, ref = refDate)
                    ?: pickExpiryYear(parsed, centuryBase = 1900, ref = refDate)
                    ?: return rawOnly(rawYear, rawMonth, rawDay, componentsFormCalendarDate = componentsFormCalendar)
            return MrzDate(
                rawYear = rawYear,
                rawMonth = rawMonth,
                rawDay = rawDay,
                computedYear = pickedYear,
                computedDate = LocalDate(pickedYear, parsed.month, parsed.day),
                inferenceMethod = MrzDateInferenceMethod.SLIDING_WINDOW_EXPIRY,
                componentsFormCalendarDate = true,
            )
        }

        private fun rawOnly(
            rawYear: String,
            rawMonth: String,
            rawDay: String,
            componentsFormCalendarDate: Boolean?,
            componentsExceedBirthAgeLimit: Boolean? = null,
        ): MrzDate =
            MrzDate(
                rawYear = rawYear,
                rawMonth = rawMonth,
                rawDay = rawDay,
                computedYear = null,
                computedDate = null,
                inferenceMethod = MrzDateInferenceMethod.RAW_ONLY,
                componentsFormCalendarDate = componentsFormCalendarDate,
                componentsExceedBirthAgeLimit = componentsExceedBirthAgeLimit,
            )

        private data class ParsedComponents(
            val twoDigitYear: Int,
            val month: Int,
            val day: Int,
        )

        private fun parseRawComponents(
            rawYear: String,
            rawMonth: String,
            rawDay: String,
        ): ParsedComponents? {
            if (rawYear.length != 2 || rawMonth.length != 2 || rawDay.length != 2) return null
            val y = rawYear.toIntOrNull() ?: return null
            val m = rawMonth.toIntOrNull() ?: return null
            val d = rawDay.toIntOrNull() ?: return null
            return ParsedComponents(y, m, d)
        }

        private fun anyCenturyFormsCalendarDate(parsed: ParsedComponents): Boolean =
            tryConstructDate(2000 + parsed.twoDigitYear, parsed.month, parsed.day) != null ||
                tryConstructDate(1900 + parsed.twoDigitYear, parsed.month, parsed.day) != null

        private fun anyBirthCandidateInPast(
            parsed: ParsedComponents,
            ref: LocalDate,
        ): Boolean {
            for (centuryBase in listOf(2000, 1900)) {
                val candidateYear = centuryBase + parsed.twoDigitYear
                val candidateDate = tryConstructDate(candidateYear, parsed.month, parsed.day) ?: continue
                if (candidateDate <= ref) return true
            }
            return false
        }

        private fun pickBirthYear(
            parsed: ParsedComponents,
            centuryBase: Int,
            ref: LocalDate,
        ): Int? {
            val candidateYear = centuryBase + parsed.twoDigitYear
            val candidateDate = tryConstructDate(candidateYear, parsed.month, parsed.day) ?: return null
            if (candidateDate > ref) return null
            if (ref.year - candidateYear > MAX_PLAUSIBLE_AGE_YEARS) return null
            return candidateYear
        }

        private fun pickExpiryYear(
            parsed: ParsedComponents,
            centuryBase: Int,
            ref: LocalDate,
        ): Int? {
            val candidateYear = centuryBase + parsed.twoDigitYear
            tryConstructDate(candidateYear, parsed.month, parsed.day) ?: return null
            if (candidateYear < ref.year - EXPIRY_PAST_WINDOW_YEARS) return null
            if (candidateYear > ref.year + EXPIRY_FUTURE_WINDOW_YEARS) return null
            return candidateYear
        }

        private fun tryConstructDate(
            year: Int,
            month: Int,
            day: Int,
        ): LocalDate? =
            try {
                LocalDate(year, month, day)
            } catch (_: IllegalArgumentException) {
                null
            }
    }
}
