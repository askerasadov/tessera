package io.lightine.tessera.types.vocabulary

import kotlin.test.Test
import kotlin.test.assertEquals

class MrzFormatTest {
    @Test
    fun exposes_the_five_icao_doc_9303_formats() {
        assertEquals(
            listOf("TD1", "TD2", "TD3", "MRV_A", "MRV_B"),
            MrzFormat.entries.map { it.name },
        )
    }
}
