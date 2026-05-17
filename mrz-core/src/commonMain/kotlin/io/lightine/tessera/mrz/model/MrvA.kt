package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.MrzFormat

/**
 * A parsed MRV-A (Type-A visa) MRZ document per ICAO Doc 9303 Part 7. Two lines of 44
 * characters. Adds [optionalData] (positions 28–44 on line 2) beyond the [CommonFields]
 * shared with other formats.
 *
 * Unlike TD3 (same line dimensions), MRV-A does not have a composite check digit per
 * ICAO Doc 9303 Part 7 — the last 16 characters of line 2 are entirely optional data.
 */
public data class MrvA(
    override val rawLines: List<String>,
    override val commonFields: CommonFields,
    val optionalData: String,
) : MrzDocument() {
    override val format: MrzFormat = MrzFormat.MRV_A
}
