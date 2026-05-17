package io.lightine.tessera.domain.vocabulary

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentCategoryTest {
    @Test
    fun exposes_passport_identity_card_residence_permit_visa_other() {
        assertEquals(
            listOf("PASSPORT", "IDENTITY_CARD", "RESIDENCE_PERMIT", "VISA", "OTHER"),
            DocumentCategory.entries.map { it.name },
        )
    }
}
