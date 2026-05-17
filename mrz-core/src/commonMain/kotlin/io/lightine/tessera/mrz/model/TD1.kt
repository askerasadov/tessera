package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.MrzFormat

/**
 * A parsed TD1 (identity card) MRZ document per ICAO Doc 9303 Part 5. Three lines of 30
 * characters. Adds [optionalData1] (positions 15–30 on line 1) and [optionalData2]
 * (positions 18–29 on line 2) beyond the [CommonFields] shared with other formats.
 */
public data class TD1(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val optionalData1: String,
    val optionalData2: String,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.TD1
}
