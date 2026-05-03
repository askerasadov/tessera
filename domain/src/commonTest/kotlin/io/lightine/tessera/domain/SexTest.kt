package io.lightine.tessera.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SexTest {
    @Test
    fun exposes_exactly_male_female_unspecified() {
        assertEquals(
            listOf("MALE", "FEMALE", "UNSPECIFIED"),
            Sex.entries.map { it.name },
        )
    }
}
