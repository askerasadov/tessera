# MRZ Generation

This feature document describes the SDK's MRZ generation capability: turning structured input into a valid MRZ string. Generation is the inverse of parsing and serves three primary use cases: producing test fixtures, supporting document issuance flows, and round-trip verification.

This document focuses on the SDK-specific design choices: the public API shape, how transliteration is invoked, how field overflow is handled, and the round-trip guarantees the generator commits to. The byte-level format specifications themselves live in ICAO Doc 9303.

**Status:** Living
**Available since:** 0.1.0
**Platform availability:** Target-agnostic. Generation is pure logic and runs on every target the project supports.

---

## Purpose

The generator produces a syntactically valid MRZ string from structured input. It computes check digits, applies field padding, and produces output that conforms to ICAO Doc 9303 for the requested format. If the input cannot produce a conformant output, the generator fails with a typed error rather than producing invalid output silently (Principle 7 — Fail loudly, fail informatively).

Generation enables several capabilities that pure parsing cannot:

- Constructing test fixtures programmatically — synthetic MRZs covering edge cases, used in unit tests and integration tests
- Supporting document issuance flows where structured data must be encoded into a valid MRZ for printing
- Round-trip verification — generating an MRZ from a parsed document and confirming the result matches the original input

The generator is always paired with the parser. The data model defined in `mrz-data-model.md` is the canonical type both operate on.

---

## What Generation Does

Given valid structured input for a specific format, the generator:

1. Validates that the input fits the field widths defined for the format
2. Applies transliteration to name fields if a transliteration profile is provided
3. Encodes each field per ICAO Doc 9303 rules for the format
4. Computes check digits per the algorithm in Doc 9303 Part 3 Appendix A
5. Pads fields with the filler character `<` as required
6. Returns the produced MRZ as a list of strings (one per MRZ line)

What the generator does *not* do:

- Guess what locale or transliteration profile the consumer wants
- Silently truncate input that does not fit the field width (it fails with a typed error, with one exception — see "Long Document Number" below)
- Apply transliteration unless explicitly directed to do so
- Produce non-conformant output ("best effort" generation is not supported)

---

## Public API Shape

The generator exposes per-format methods. Each format has its own signature reflecting the fields specific to that format. There is no single `generate(format, ...)` method because format-specific signatures are clearer and safer.

The illustrative shape:

```
object MrzGenerator {
    // Per-format methods accepting primitive inputs
    fun generateTD1(
        documentType: String,
        issuingState: String,
        documentNumber: String,
        // ... TD1-specific fields
    ): GenerationResult

    fun generateTD3(
        documentType: String,
        issuingState: String,
        documentNumber: String,
        primaryIdentifier: String,
        secondaryIdentifier: String,
        nationality: String,
        dateOfBirth: MrzDateInput,
        sex: Sex,
        dateOfExpiry: MrzDateInput,
        personalNumber: String,
        // ... optional transliteration profile
        transliteration: TransliterationProfile? = null
    ): GenerationResult

    // ... analogous methods for TD2, MRV-A, MRV-B

    // Per-format methods accepting the data model directly
    fun generate(document: MrzDocument.TD1): GenerationResult
    fun generate(document: MrzDocument.TD2): GenerationResult
    fun generate(document: MrzDocument.TD3): GenerationResult
    fun generate(document: MrzDocument.MrvA): GenerationResult
    fun generate(document: MrzDocument.MrvB): GenerationResult
}
```

Both input forms are supported because both are natural in different consumer contexts:

- **Primitive inputs** are used when constructing an MRZ from scratch — typical of test fixtures, document issuance flows, and consumers who do not have a parsed `MrzDocument` instance
- **Data model inputs** are used in round-trip flows — parsing an MRZ, modifying the result, and generating a new MRZ from the modified document

Per-format methods exist because each format has meaningfully different required fields. A single method with a giant union of all possible fields would be confusing and error-prone. Per-format signatures give each format a clean, type-safe contract.

The actual class names, method names, and parameter shapes are decided at implementation time. The shape above is illustrative.

---

## Source of Truth for Format Definitions

Format specifications — field positions, field widths, check digit positions, padding rules — live in a single shared definition within `mrz-core`. The parser, generator, and validator all reference this source rather than each maintaining their own copy. Changing a definition in one place updates all three subsystems consistently.

This is an internal architectural commitment that supports Principle 3 (Modular) and Principle 9 (Forward-compatible API): if ICAO updates a format specification, the change happens in one place. Adding a new format follows a documented procedure (see `conventions.md`).

---

## Input Validation

Before producing any output, the generator validates that the input can produce a conformant MRZ. This includes:

- Required fields are present and non-empty (where required)
- Field values fit within the defined field widths
- Country codes and document type codes are within the allowed character set (the recognition against lookup tables is informational, not a generation gate; an unrecognized but well-formed code is accepted)
- Date fields parse to real calendar dates
- Sex values are within the allowed set (`MALE`, `FEMALE`, `UNSPECIFIED`)
- Names contain only characters in the MRZ alphabet (A-Z and the filler), unless a transliteration profile is provided

Validation failures at this stage produce typed errors and the generator does not produce output. The consumer receives `GenerationResult.Failure` with a specific error type indicating what was wrong.

---

## Transliteration Behavior

Names in the MRZ must use only the restricted MRZ alphabet (uppercase A-Z and the filler character). Real-world names often contain characters outside this set: diacritics, characters from Cyrillic or other scripts, the Latin schwa, and so on.

The generator handles this through explicit consumer choice. Two paths are supported:

### Path 1 — Pre-Transliterated Input

The consumer transliterates names themselves before calling the generator. The input strings already contain only MRZ-alphabet characters. The generator accepts them directly and produces output. No transliteration profile is needed.

This path is used when the consumer has its own transliteration logic, or when the names are already in MRZ form (for example, in round-trip flows where the names came from a parsed MRZ).

### Path 2 — Generator-Applied Transliteration

The consumer passes original names (which may contain non-MRZ-alphabet characters) along with a `TransliterationProfile`. The generator applies the profile to produce MRZ-compatible names, then encodes them.

This path is used when the consumer wants the SDK to handle transliteration. The profile must be specified explicitly — the generator never guesses which profile applies based on issuing state or any other input. This is consistent with Principle 1 (Reader, not oracle): the SDK does not infer locale.

If the consumer provides a name with non-MRZ-alphabet characters and no profile, the generator returns a typed error (`MrzGenerationUnsupportedCharacters`). The consumer must either pre-transliterate the name or provide a profile.

---

## Long Document Number Extension

ICAO Doc 9303 Part 4 (TD3 / passports) defines an extension for document numbers that exceed 9 characters: excess characters spill into the personal number field, with a specific marker indicating the overflow. This is the "long document number" extension, and it appears in real-world passports.

The TD3 generator implements this extension correctly. When the consumer provides a document number longer than 9 characters, the generator:

1. Places the first 9 characters in the document number field
2. Places the remaining characters at the start of the personal number field
3. Inserts the appropriate marker character to indicate the overflow
4. Adjusts the personal number length accordingly
5. Computes check digits over the resulting layout

The extension is supported only for TD3, because that is the only format where ICAO defines it. For TD1, TD2, MRV-A, and MRV-B, document numbers exceeding the field width produce a typed error (`MrzGenerationFieldOverflow`).

---

## Round-Trip Guarantees

The generator commits to round-trip equality at the raw-field level: parsing a generator-produced MRZ yields a `MrzDocument` whose raw fields match the input data passed to the generator.

Specifically:

- A generator call with a `MrzDocument.TD3` input produces an MRZ that, when parsed, yields a `MrzDocument.TD3` with raw fields equal to the original
- A generator call with primitive inputs produces an MRZ that, when parsed, yields a `MrzDocument` with the same raw fields as the inputs

The "raw fields" qualifier matters. Computed fields (like `MrzDate.computedYear`) are derived from raw fields and the current time; they are deterministic given the same time, but not part of the round-trip contract because they may change with passing time. The contract is on raw values: if you put `25` as the year, the round-tripped MRZ produces `25` as the raw year.

This guarantee is what makes the generator usable as a testing oracle. A property-based test can generate random valid inputs, run them through generation and back through parsing, and assert equality of raw fields. Any divergence indicates a bug in either the generator or the parser.

---

## Behavioral Commitments

The generator commits to the following behaviors. These are part of the public contract.

### Strict Conformance

Output is strictly conformant to ICAO Doc 9303 for the requested format. The generator does not produce best-effort or "almost valid" MRZs. Any input that cannot produce conformant output results in a typed error.

### Deterministic Output

Generation is deterministic. The same inputs always produce the same MRZ string, regardless of when or where the generator is invoked. There is no time-dependent behavior in generation (unlike parsing's date inference); the generator works entirely with the raw values it is given.

### Safe to Call Concurrently

The generator is stateless. Multiple invocations can run concurrently in any threading or async model the target language supports.

### No Refusal Based on Recognition

The generator validates that field values fit and that codes are well-formed. It does not refuse based on whether codes are recognized in the lookup tables. A consumer generating an MRZ with an unusual but well-formed country code (such as a future code not yet in our tables) is supported.

---

## Relationship to Other Features

- **Data model** (`mrz-data-model.md`) — the generator accepts and produces values of types defined there
- **Error taxonomy** (`mrz-error-taxonomy.md`) — the errors the generator produces are defined there
- **Parsing** (`mrz-parsing.md`) — the inverse operation; round-trip equality is the joint contract
- **Validation** (`mrz-validation.md`) — the generator performs structural validation as part of its work; deeper validation can be invoked separately on already-generated output
- **Lookup tables** (`lookup-tables.md`) — used informationally by the generator for code recognition
- **Transliteration** (`transliteration.md`) — invoked by the generator when a profile is provided

---

## Edge Cases Worth Calling Out

A few cases that deserve explicit mention:

### Empty Optional Fields

When a format defines an optional field (such as TD3's personal number or TD1's optional data fields) and the consumer does not provide a value, the generator fills the field with the appropriate filler characters and computes the check digit accordingly. This is correct ICAO conformance, not an error.

### Sex Field Encoding

The `Sex.UNSPECIFIED` value can be encoded as either the filler character `<` or as `X` per ICAO Doc 9303. The generator uses `<` by default, consistent with most issuing states. Future configuration may allow choosing `X` explicitly; for now, `<` is used.

### Date Encoding

Generation accepts dates in their full four-digit-year form (a `LocalDate` or platform-equivalent). The generator extracts the last two digits of the year for MRZ encoding. This avoids the century inference ambiguity that affects parsing — generation always knows the full year because the consumer provides it.

### Composite Check Digit

The composite check digit (where defined for the format) is computed over multiple fields. The generator computes it correctly per the format's specification. This is a structural commitment; consumers do not need to think about composite check digits.

---

## Related Principles

- **Principle 1 (Reader, not oracle)** — applies inversely: the generator does not invent. It encodes what the consumer provides, fails when it cannot, and never guesses transliteration profiles
- **Principle 5 (Transparency)** — the generator's behavior is fully predictable from documented inputs; output structure exactly reflects what the consumer asked for
- **Principle 7 (Fail loudly, fail informatively)** — typed errors for any condition where conformant output cannot be produced
- **Principle 9 (Forward-compatible API)** — per-format methods are designed to extend through addition; new formats add new methods without changing existing ones

---

## Related Documents

- `principles.md` — the foundational principles this document references
- `mrz-data-model.md` — the types the generator accepts and produces
- `mrz-error-taxonomy.md` — the errors the generator surfaces
- `mrz-parsing.md` — the inverse operation
- `mrz-validation.md` — additional validation invokable on already-generated output
- `lookup-tables.md` — codes referenced by the generator
- `transliteration.md` — profiles the generator can apply
- `conventions.md` — the procedure for adding new formats
