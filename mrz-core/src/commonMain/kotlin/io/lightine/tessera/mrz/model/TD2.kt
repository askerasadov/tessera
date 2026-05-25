package io.lightine.tessera.mrz.model

import io.lightine.tessera.types.vocabulary.MrzFormat

/**
 * A parsed TD2 (smaller identity document) MRZ document per ICAO Doc 9303 Part 6. Two
 * lines of 36 characters. Adds [optionalData] (positions 28–35 on line 2) beyond the
 * [CommonFields] shared with other formats.
 */
public data class TD2(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val optionalData: String,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.TD2
}
