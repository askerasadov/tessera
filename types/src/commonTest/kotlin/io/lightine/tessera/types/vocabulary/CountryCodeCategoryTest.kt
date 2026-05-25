package io.lightine.tessera.types.vocabulary

import kotlin.test.Test
import kotlin.test.assertEquals

class CountryCodeCategoryTest {
    @Test
    fun exposes_state_organization_stateless_refugee_historical_other() {
        assertEquals(
            listOf("STATE", "ORGANIZATION", "STATELESS", "REFUGEE", "HISTORICAL", "OTHER"),
            CountryCodeCategory.entries.map { it.name },
        )
    }
}
