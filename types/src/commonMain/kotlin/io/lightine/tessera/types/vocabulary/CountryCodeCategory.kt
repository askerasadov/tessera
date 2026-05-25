package io.lightine.tessera.types.vocabulary

/**
 * Categorization of a country code recognized by the SDK. Returned by
 * [`CountryCode.category`][io.lightine.tessera.mrz.recognition.CountryCode.category]
 * when the code is found in the
 * [`CountryCodeTable`][io.lightine.tessera.mrz.recognition.CountryCodeTable].
 *
 * Most MRZ-eligible codes fall under [STATE] (ISO 3166-1 alpha-3 country codes). The
 * remaining categories cover the special-purpose codes defined by ICAO Doc 9303 Part 3
 * Section 5 — codes for international organizations issuing UN-style documents, codes
 * for stateless persons and refugees, and codes inherited from countries that have since
 * dissolved or changed identity.
 */
public enum class CountryCodeCategory {
    /** A code from ISO 3166-1 alpha-3 representing a recognized state. */
    STATE,

    /**
     * A code reserved by ICAO for international organizations issuing UN-style travel
     * documents (e.g., UN agencies).
     */
    ORGANIZATION,

    /** A code reserved for stateless persons per ICAO Doc 9303. */
    STATELESS,

    /** A code reserved for refugees per ICAO Doc 9303. */
    REFUGEE,

    /**
     * A code for a state that no longer exists or has changed identity but whose
     * previously-issued documents may still be circulating.
     */
    HISTORICAL,

    /** A recognized code that does not fit any of the more specific categories above. */
    OTHER,
}
