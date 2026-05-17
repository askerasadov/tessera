package io.lightine.tessera.domain.errors

import io.lightine.tessera.domain.vocabulary.MrzField
import io.lightine.tessera.domain.vocabulary.MrzFormat
import io.lightine.tessera.domain.vocabulary.UnmappedCharacter

public data class MrzGenerationUnsupportedCharacters(
    val format: MrzFormat,
    val field: MrzField,
    val unmappedCharacters: List<UnmappedCharacter>,
    val observedValue: String,
) : MrzGenerationError() {
    override val description: String
        get() {
            val chars = unmappedCharacters.joinToString(", ") { "'${it.character}' at position ${it.position}" }
            return "Format $format field ${this.field} contains characters outside the MRZ alphabet " +
                "and no transliteration profile resolved them: $chars (observed value: \"$observedValue\")"
        }
}
