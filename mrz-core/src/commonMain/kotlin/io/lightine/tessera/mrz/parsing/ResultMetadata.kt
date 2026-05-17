package io.lightine.tessera.mrz.parsing

import io.lightine.tessera.domain.errors.MrzValidationError
import io.lightine.tessera.domain.errors.MrzWarning
import io.lightine.tessera.domain.vocabulary.ReadMethod
import io.lightine.tessera.mrz.transliteration.TransliterationDetails

public data class ResultMetadata(
    val readMethod: ReadMethod,
    val warnings: List<MrzWarning>,
    val validationFailures: List<MrzValidationError>,
    val transliterationDetails: TransliterationDetails? = null,
)
