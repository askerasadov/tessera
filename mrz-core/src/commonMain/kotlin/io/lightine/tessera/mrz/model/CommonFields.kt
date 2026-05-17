package io.lightine.tessera.mrz.model

import io.lightine.tessera.domain.vocabulary.Sex
import io.lightine.tessera.mrz.recognition.CountryCode
import io.lightine.tessera.mrz.recognition.DocumentType

/**
 * The set of MRZ fields present in every supported format. Concrete
 * [`MrzDocument`][MrzDocument] subtypes hold a [CommonFields] plus any format-specific
 * extras.
 *
 * Field semantics:
 * - [primaryIdentifier], [secondaryIdentifier] — the decoded name components from
 *   ICAO Doc 9303's `<<`-separated name field. Filler `<` characters are decoded as
 *   spaces. Apostrophes and hyphens are lossy under the ICAO encoding; consumers who
 *   need the original presentation should use [rawNameField].
 * - [nameTruncated] — `true` when the name field fills the available width with no
 *   trailing filler (ICAO convention: a complete name always leaves at least one trailing
 *   `<`). See [`MrzNameTruncated`][io.lightine.tessera.domain.errors.MrzNameTruncated].
 * - [rawNameField] — the name field exactly as encoded on the document, before decoding.
 * - [sex] — the SDK's [`Sex`][io.lightine.tessera.domain.vocabulary.Sex] enum, derived
 *   from [rawSex]. Both are exposed per Principle 5 (transparency).
 * - [rawSex] — the actual character on the document. May be outside the ICAO allowed
 *   set if the document is non-conforming; see
 *   [`MrzInvalidSexValue`][io.lightine.tessera.domain.errors.MrzInvalidSexValue].
 * - [checkDigits] — the check digits as recorded on the document, not as the SDK would
 *   compute them. See [MrzCheckDigits].
 */
public data class CommonFields(
    val documentType: DocumentType,
    val issuingState: CountryCode,
    val primaryIdentifier: String,
    val secondaryIdentifier: String,
    val nameTruncated: Boolean,
    val rawNameField: String,
    val documentNumber: String,
    val nationality: CountryCode,
    val dateOfBirth: MrzDate,
    val sex: Sex,
    val rawSex: Char,
    val dateOfExpiry: MrzDate,
    val checkDigits: MrzCheckDigits,
)
