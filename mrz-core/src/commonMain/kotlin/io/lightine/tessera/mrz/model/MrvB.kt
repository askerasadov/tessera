package io.lightine.tessera.mrz.model

import io.lightine.tessera.types.vocabulary.MrzFormat

/**
 * A parsed MRV-B (Type-B visa) MRZ document per ICAO Doc 9303 Part 7. Two lines of 36
 * characters. Adds [optionalData] (positions 28–36 on line 2) beyond the [CommonFields]
 * shared with other formats.
 *
 * Unlike TD2 (same line dimensions), MRV-B does not have a composite check digit per
 * ICAO Doc 9303 Part 7 — the last 8 characters of line 2 are entirely optional data.
 */
public data class MrvB(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val optionalData: String,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.MRV_B
}
