package io.lightine.tessera.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzExpiryDateImplausiblyFarTest {
    @Test
    fun stores_dates_and_threshold_verbatim() {
        val warning =
            MrzExpiryDateImplausiblyFar(
                expiryDate = LocalDate(2040, 1, 1),
                referenceDate = LocalDate(2026, 5, 4),
                thresholdYears = 10,
            )
        assertEquals(LocalDate(2040, 1, 1), warning.expiryDate)
        assertEquals(LocalDate(2026, 5, 4), warning.referenceDate)
        assertEquals(10, warning.thresholdYears)
    }

    @Test
    fun description_names_dates_and_threshold() {
        val warning =
            MrzExpiryDateImplausiblyFar(
                expiryDate = LocalDate(2040, 1, 1),
                referenceDate = LocalDate(2026, 5, 4),
                thresholdYears = 10,
            )
        val description = warning.description
        assertTrue("2040-01-01" in description, "Description should mention expiry date; got '$description'")
        assertTrue("2026-05-04" in description, "Description should mention reference date; got '$description'")
        assertTrue("10" in description, "Description should mention threshold; got '$description'")
    }

    @Test
    fun is_a_warning() {
        val warning: MrzWarning =
            MrzExpiryDateImplausiblyFar(
                expiryDate = LocalDate(2050, 1, 1),
                referenceDate = LocalDate(2026, 1, 1),
                thresholdYears = 10,
            )
        assertTrue(warning is MrzExpiryDateImplausiblyFar)
    }
}
