package io.lightine.tessera.mrz.transliteration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TransliterationProfileRegistryTest {
    @Test
    fun icao_default_is_pre_registered() {
        val looked = TransliterationProfileRegistry.lookup(IcaoDefaultTransliterationProfile.IDENTIFIER)
        assertNotNull(looked)
        assertSame(IcaoDefaultTransliterationProfile, looked)
    }

    @Test
    fun lookup_of_unknown_identifier_returns_null() {
        assertNull(TransliterationProfileRegistry.lookup("DEFINITELY-NOT-A-PROFILE-Z9X"))
    }

    @Test
    fun register_adds_a_new_profile() {
        val custom = stubProfile("TEST-REG-ADD")
        TransliterationProfileRegistry.register(custom)
        assertSame(custom, TransliterationProfileRegistry.lookup("TEST-REG-ADD"))
    }

    @Test
    fun register_replaces_by_identifier() {
        val first = stubProfile("TEST-REG-REPLACE", outputTag = "FIRST")
        val second = stubProfile("TEST-REG-REPLACE", outputTag = "SECOND")
        TransliterationProfileRegistry.register(first)
        TransliterationProfileRegistry.register(second)
        val looked = TransliterationProfileRegistry.lookup("TEST-REG-REPLACE")
        assertSame(second, looked)
    }

    @Test
    fun all_includes_icao_default_plus_registered_profiles() {
        val custom = stubProfile("TEST-REG-ALL")
        TransliterationProfileRegistry.register(custom)
        val all = TransliterationProfileRegistry.all()
        assertTrue(all.any { it.identifier == IcaoDefaultTransliterationProfile.IDENTIFIER })
        assertTrue(all.any { it.identifier == "TEST-REG-ALL" })
    }

    @Test
    fun all_returns_snapshot_independent_of_subsequent_registrations() {
        val snapshot = TransliterationProfileRegistry.all()
        val snapshotSize = snapshot.size
        TransliterationProfileRegistry.register(stubProfile("TEST-REG-SNAPSHOT"))
        // Snapshot taken before the registration is unaffected.
        assertEquals(snapshotSize, snapshot.size)
    }

    private fun stubProfile(
        id: String,
        outputTag: String = "STUB",
    ): TransliterationProfile =
        object : TransliterationProfile {
            override val identifier: String = id

            override fun toMrzAlphabet(normalizedInput: String): TransliterationResult = TransliterationResult.Success(outputTag)
        }
}
