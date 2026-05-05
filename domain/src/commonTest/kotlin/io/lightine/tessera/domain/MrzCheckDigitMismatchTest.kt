package io.lightine.tessera.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MrzCheckDigitMismatchTest {
    @Test
    fun stores_field_expected_observed_and_position_verbatim() {
        val error =
            MrzCheckDigitMismatch(
                field = MrzField.DOCUMENT_NUMBER,
                expected = '3',
                observed = '7',
                position = 9,
            )
        assertEquals(MrzField.DOCUMENT_NUMBER, error.field)
        assertEquals('3', error.expected)
        assertEquals('7', error.observed)
        assertEquals(9, error.position)
    }

    @Test
    fun description_names_field_expected_observed_and_position() {
        val error =
            MrzCheckDigitMismatch(
                field = MrzField.DATE_OF_EXPIRY,
                expected = '6',
                observed = '5',
                position = 71,
            )
        val description = error.description
        assertTrue("DATE_OF_EXPIRY" in description, "Description should name the field; got '$description'")
        assertTrue("'6'" in description, "Description should mention expected; got '$description'")
        assertTrue("'5'" in description, "Description should mention observed; got '$description'")
        assertTrue("71" in description, "Description should mention position; got '$description'")
    }

    @Test
    fun is_a_validation_error() {
        val error: MrzValidationError =
            MrzCheckDigitMismatch(
                field = MrzField.COMPOSITE,
                expected = '4',
                observed = '0',
                position = 87,
            )
        assertTrue(error is MrzCheckDigitMismatch)
    }
}
