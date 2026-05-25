package io.lightine.tessera.mrz.transliteration

import io.lightine.tessera.types.vocabulary.UnmappedCharacter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TransliterationResultTest {
    @Test
    fun success_equality_by_output() {
        assertEquals(TransliterationResult.Success("FOO"), TransliterationResult.Success("FOO"))
        assertNotEquals(TransliterationResult.Success("FOO"), TransliterationResult.Success("BAR"))
    }

    @Test
    fun failure_equality_by_unmapped_list() {
        val a = TransliterationResult.Failure(listOf(UnmappedCharacter('x', 0)))
        val b = TransliterationResult.Failure(listOf(UnmappedCharacter('x', 0)))
        val c = TransliterationResult.Failure(listOf(UnmappedCharacter('x', 1)))
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun a_profile_can_surface_unmapped_characters_via_failure() {
        // A failing profile that only knows about the letter 'A'; everything else is unmapped.
        val profile =
            object : TransliterationProfile {
                override val identifier: String = "TEST-A-ONLY"

                override fun toMrzAlphabet(normalizedInput: String): TransliterationResult {
                    val unmapped = mutableListOf<UnmappedCharacter>()
                    for ((position, char) in normalizedInput.withIndex()) {
                        if (char != 'A') unmapped += UnmappedCharacter(char, position)
                    }
                    return if (unmapped.isEmpty()) {
                        TransliterationResult.Success(normalizedInput)
                    } else {
                        TransliterationResult.Failure(unmapped)
                    }
                }
            }

        val result = profile.toMrzAlphabet("AxAy")
        assertEquals(
            TransliterationResult.Failure(
                listOf(
                    UnmappedCharacter('x', 1),
                    UnmappedCharacter('y', 3),
                ),
            ),
            result,
        )
    }
}
