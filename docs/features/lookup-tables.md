# Lookup Tables

This feature document describes the reference data the SDK ships with: country codes, document type codes, and the structure used to look them up. The lookup tables are the source of truth for code recognition; they inform parsing, validation, and generation.

This document focuses on the SDK-specific design choices: what data is included, how it is structured, how recognition is exposed, and how the tables evolve over time. The actual code values themselves are defined by ICAO Doc 9303 and ISO 3166-1; the SDK does not redefine them.

**Status:** Living
**Available since:** 0.1.0
**Platform availability:** Target-agnostic. Lookup tables are pure data and run on every target the project supports.

---

## Purpose

Several parts of the SDK need to recognize codes that appear in the MRZ:

- The parser needs to identify country and document type codes for the data model's recognition flags
- The validator needs to check whether codes appear in the recognized lists
- The generator needs to validate that codes are well-formed (the actual recognition is informational, not a generation gate)

Centralizing this reference data in dedicated lookup tables keeps it consistent across all three subsystems, makes updates trivial (new codes are added in one place), and exposes the data to consumers who want to inspect it directly (Principle 5 — Transparency).

---

## Country and Nationality Codes

The MRZ uses three-letter codes to identify the issuing state and the holder's nationality. These codes follow ISO 3166-1 alpha-3 with extensions defined in ICAO Doc 9303 Part 3 Section 5.

The SDK ships a lookup table containing:

- All ISO 3166-1 alpha-3 codes
- ICAO-specific extensions, including codes for organizations that issue travel documents but are not countries (such as the United Nations, the Sovereign Military Order of Malta, and others)
- Codes representing stateless persons, refugees, and other non-state cases as defined in ICAO Doc 9303
- Historical codes that may still appear on documents in circulation (for example, codes for predecessor states whose documents have not yet expired)

Each entry in the table contains:

- The three-letter code itself
- An English display name
- A category (`STATE`, `ORGANIZATION`, `STATELESS`, `REFUGEE`, `HISTORICAL`, `OTHER`)
- An optional note when the code has special meaning or context

The lookup table is exposed publicly, not just used internally. Consumers can iterate over the full set of codes, look up a specific code, or check whether a code is recognized — without needing to fork the SDK or maintain their own copy of the data.

---

## Document Type Codes

The first character or two of the MRZ identifies the document type. ICAO Doc 9303 defines two generations of these codes:

- **Single-character codes** — the original system. `P` for passport, `V` for visa, `I` / `A` / `C` for various ID-card-like documents
- **Two-character codes** — the current system, in active use as of recent ICAO Doc 9303 editions. `PP` for ordinary passport, `PD` for diplomatic, `PS` for service, `PE` for emergency, and so on

The SDK ships a lookup table containing both generations. Each entry contains:

- The code itself (one or two characters)
- An English display name
- A category (`PASSPORT`, `IDENTITY_CARD`, `RESIDENCE_PERMIT`, `VISA`, `OTHER`)
- The generation (`LEGACY_SINGLE_CHARACTER`, `CURRENT_TWO_CHARACTER`)

The lookup is unified: a parser receiving a one-character code looks it up the same way it looks up a two-character code. The result indicates the generation as part of the entry.

Recognition is informational, not gating. A document type code that is not in the lookup table is exposed verbatim by the parser, with a flag indicating it is unrecognized. The parser does not refuse such codes (Principle 1 — Reader, not oracle).

---

## Structure and Public API

The lookup tables are exposed as immutable data structures with read-only access. The illustrative shape:

```
object CountryCodeTable {
    fun lookup(code: String): CountryCodeEntry?
    fun all(): List<CountryCodeEntry>
    fun byCategory(category: CountryCodeCategory): List<CountryCodeEntry>
}

object DocumentTypeCodeTable {
    fun lookup(code: String): DocumentTypeCodeEntry?
    fun all(): List<DocumentTypeCodeEntry>
    fun byCategory(category: DocumentCategory): List<DocumentTypeCodeEntry>
}
```

The tables are pure data — they do not change at runtime, do not depend on any external resource, and do not involve I/O. They compile into the SDK as constant data.

The actual class names, method names, and parameter shapes are decided at implementation time. The shape above is illustrative.

---

## Recognition Semantics

When a code appears in the MRZ, the SDK distinguishes two states:

- **Recognized** — the code is in the lookup table; the entry's display name and category are available
- **Unrecognized** — the code is not in the lookup table; the raw code is exposed and the consumer can decide what to do

This is the only meaning of "recognized" in the SDK. It does not mean "valid" or "trustworthy"; ICAO updates these lists periodically, and a code may be valid but newer than the SDK's tables. A consumer who needs strict recognition can treat unrecognized codes as disqualifying; a consumer who is more lenient can accept them with the verbatim value.

The recognition flag appears in the data model on the relevant value classes (`CountryCode`, `DocumentType`). The validator surfaces unrecognized codes as warnings (`MrzUnknownCountryCode`, `MrzUnknownDocumentTypeCode`), not errors.

---

## Updating the Tables

ICAO updates the official code lists periodically, and the SDK's tables must keep pace. Updates follow the project's versioning rules:

- Adding a new code to a table is a non-breaking change; it can ship in a MINOR or PATCH release
- Removing a code is breaking only if the removal causes consumer code to behave differently for input that previously contained the code; in practice, a code already published by a state is rarely "removed" by ICAO, so this case is rare
- Renaming the display name of an entry is non-breaking; the code itself is the stable identifier
- Changing the category of an entry is non-breaking but documented in the changelog as it may affect consumer logic that filters by category

The procedure for adding or updating codes is documented in `conventions.md`. The tables themselves are part of the `mrz-core` module's data, kept synchronized with ICAO publications.

---

## Behavioral Commitments

The lookup tables commit to the following behaviors. These are part of the public contract.

### Immutable

The tables are immutable at runtime. Consumers cannot modify them. This ensures that the SDK's internal logic and consumer-facing code see the same data, always.

### Available Without I/O

The tables are compiled into the SDK as data. No network, file system, or external service access is required to use them. This keeps the SDK fully usable in offline contexts (Principle 10 — Privacy by default; no phone-home).

### Stable Across a Major Version

Within a single MAJOR version, codes are added freely (non-breaking). Removals follow the versioning rules. Consumers can rely on a code that exists in version N still existing in version N+0.1, N+0.2, etc.

### Recognition Is Informational

Recognition is exposed as data on the relevant types, never used as a gate for parsing or generation. An unrecognized code is parsed and exposed verbatim; an unrecognized code provided to the generator is encoded verbatim (provided it fits the field width and uses only MRZ-alphabet characters).

---

## Coverage and Caveats

The SDK ships the lookup tables it ships. A few things worth being explicit about:

### Initial Country Code Coverage

The initial release includes the full ISO 3166-1 alpha-3 list and the ICAO-specific extensions listed in ICAO Doc 9303 Part 3 Section 5 at the time of the SDK release. Codes that ICAO publishes later are added in subsequent SDK releases.

### Initial Document Type Code Coverage

The initial release includes the full set of single-character and two-character codes documented in current ICAO Doc 9303 editions. Country-specific document type variants (where a state uses a code with locally meaningful interpretation) are not included unless they appear in the official ICAO documentation.

### English Names Only

Display names are English. Consumers building user-facing experiences in other languages map codes to their own translated names; the SDK provides the code-to-English-name mapping as the basis. Localization of these names is a consumer concern, consistent with the broader localization stance described in `conventions.md`.

### Historical Codes

Some codes represent predecessor states whose documents may still be in circulation (with valid expiry dates). These are included with the `HISTORICAL` category so consumers can recognize and handle them appropriately. The SDK does not decide whether a document with a historical code is "still acceptable" — that is a consumer decision.

---

## Relationship to Other Features

- **Data model** (`mrz-data-model.md`) — `CountryCode` and `DocumentType` value classes consume these tables for recognition
- **Parsing** (`mrz-parsing.md`) — the parser uses the tables to populate recognition flags
- **Validation** (`mrz-validation.md`) — the validator uses the tables to check whether codes are recognized; produces warnings for unrecognized codes
- **Generation** (`mrz-generation.md`) — the generator references the tables informationally; recognition is not a gate for generation
- **Conventions** (`conventions.md`) — the procedure for updating the tables

---

## Related Principles

- **Principle 1 (Reader, not oracle)** — recognition is informational, never a gate; verbatim codes are always preserved
- **Principle 5 (Transparency)** — the tables are exposed as public data; consumers can inspect them, iterate them, look up entries directly
- **Principle 9 (Forward-compatible API)** — table updates are non-breaking; new codes can be added without affecting consumer code
- **Principle 10 (Privacy by default)** — tables are compiled into the SDK; no I/O required

---

## Related Documents

- `principles.md` — the foundational principles this document references
- `mrz-data-model.md` — value classes that consume these tables
- `mrz-parsing.md` — how the parser uses recognition results
- `mrz-validation.md` — how unrecognized codes are surfaced
- `mrz-generation.md` — how the generator references the tables
- `conventions.md` — the procedure for updating tables
