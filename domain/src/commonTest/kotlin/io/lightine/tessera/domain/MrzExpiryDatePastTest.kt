package io.lightine.tessera.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzExpiryDatePastTest {
    @Test
    fun stores_expiry_and_reference_dates_verbatim() {
        val warning =
            MrzExpiryDatePast(
                expiryDate = LocalDate(2020, 6, 23),
                referenceDate = LocalDate(2026, 5, 4),
            )
        assertEquals(LocalDate(2020, 6, 23), warning.expiryDate)
        assertEquals(LocalDate(2026, 5, 4), warning.referenceDate)
    }

    @Test
    fun description_names_both_dates() {
        val warning =
            MrzExpiryDatePast(
                expiryDate = LocalDate(2020, 6, 23),
                referenceDate = LocalDate(2026, 5, 4),
            )
        val description = warning.description
        assertTrue("2020-06-23" in description, "Description should mention expiry date; got '$description'")
        assertTrue("2026-05-04" in description, "Description should mention reference date; got '$description'")
    }

    @Test
    fun is_a_warning() {
        val warning: MrzWarning =
            MrzExpiryDatePast(
                expiryDate = LocalDate(2020, 1, 1),
                referenceDate = LocalDate(2026, 1, 1),
            )
        assertTrue(warning is MrzExpiryDatePast)
    }
}
