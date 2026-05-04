# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Within the 0.x line, the project follows strict backward compatibility per [ADR-007](docs/decisions/0007-strict-backward-compat-from-0x.md): MINOR additions are non-breaking, and breaking changes (when they occur) bump MAJOR.

## [Unreleased]

### Added

- `domain` module: foundational vocabulary types
  - `Sex`, `MrzFormat`, `DocumentCategory`, `CountryCodeCategory` enums
  - `ReadMethod` enum (`LIVE_CAMERA`, `PRE_CAPTURED_IMAGE`, `MANUAL_ENTRY`, `NFC_CHIP`, `BACKEND_STRING_INPUT`, `MIXED`)
- `domain` module: error taxonomy sealed roots and first concrete variants
  - `MrzError` sealed root with abstract `description: String` per [ADR-012](docs/decisions/0012-recognition-types-live-with-tables.md) and the error-taxonomy doc
  - `MrzParseError` intermediate sealed root for parser errors (`MrzGenerationError` planned for the generator slice)
  - `MrzCharacterSetViolation` variant carrying offending character + position
  - `MrzInvalidLength` variant carrying format + expected/observed dimensions
  - `MrzValidationError` and `MrzWarning` empty sealed roots (referenced by `ResultMetadata`; concrete variants land with the validator slice)
- `mrz-core` module: ICAO Doc 9303 Part 3 check digit primitive
  - `computeCheckDigit(input: String): Char` with weights 7-3-1 repeating
  - Throws `IllegalArgumentException` for input outside the MRZ alphabet (the parsing layer translates this to `MrzCharacterSetViolation`)
- `mrz-core` module: MRZ alphabet primitive
  - `isMrzAlphabetCharacter(c: Char): Boolean` predicate matching A-Z, 0-9, and the filler `<`
- `mrz-core` module: data model
  - `MrzDocument` sealed class with abstract `rawLines`, `format`, `commonFields`
  - `TD3` data class — first concrete variant, two-line × 44-character passport format per ICAO Doc 9303 Part 4
  - `TD1` data class — second variant, three-line × 30-character ID card format per ICAO Doc 9303 Part 5
  - `CommonFields` aggregate of fields shared across formats
  - `MrzCheckDigits` aggregate (`documentNumber`, `dateOfBirth`, `dateOfExpiry`, `optionalData: Char?`, `composite`)
  - `MrzDate` raw + computed (`computedYear`, `computedDate: LocalDate`, `inferenceMethod`) per [ADR-008](docs/decisions/0008-date-inference-hybrid.md); `parseBirth` and `parseExpiry` companion factories with `referenceTime: Instant` parameter
  - `MrzDateInferenceMethod` enum (`SLIDING_WINDOW_BIRTH`, `SLIDING_WINDOW_EXPIRY`, `RAW_ONLY`)
- `mrz-core` module: lookup table machinery (per [ADR-012](docs/decisions/0012-recognition-types-live-with-tables.md))
  - `DocumentType` value class wrapping a raw code with `entry`, `isRecognized`, `category` accessors
  - `DocumentTypeCodeTable` object with `lookup` / `all` / `byCategory`
  - `DocumentTypeCodeEntry` data class (code, displayName, category, generation)
  - `DocumentTypeGeneration` enum (`LEGACY_SINGLE_CHARACTER`, `CURRENT_TWO_CHARACTER`)
  - Starter set of 6 codes (`P`, `V`, `I`, `PP`, `PD`, `PS`); deliberately incomplete and tracked in [`docs/open-questions.md`](docs/open-questions.md)
- `mrz-core` module: TD3 parser
  - `MrzParser.parseTD3(input: String, referenceTime: Instant)` and `MrzParser.parseTD3(input: List<String>, referenceTime: Instant)` overloads
  - String input normalizes LF/CRLF/CR line endings, drops leading empty lines, trims trailing whitespace
  - Returns `ParseResult.Success` with structurally-valid input, `ParseResult.Failure(MrzInvalidLength(...))` for wrong shape, `ParseResult.Failure(MrzCharacterSetViolation(c, position))` for non-MRZ-alphabet characters
- `mrz-core` module: result types
  - `ParseResult` sealed class with `Success`, `PartialSuccess`, `Failure` variants
  - `ResultMetadata` aggregate (`readMethod`, `warnings`, `validationFailures`)
- Build infrastructure
  - `kotlinx-datetime 0.6.1` declared as `api` dependency in `mrz-core` (transitively exposes `LocalDate`)
- Documentation
  - [`docs/decisions/0012-recognition-types-live-with-tables.md`](docs/decisions/0012-recognition-types-live-with-tables.md): ADR resolving where recognition-bearing value classes live (with their lookup tables in `mrz-core`, not `domain`)
  - "Error Sub-Categorization by Operation" section added to [`docs/features/mrz-error-taxonomy.md`](docs/features/mrz-error-taxonomy.md) documenting `MrzParseError` / `MrzGenerationError` intermediate sealed roots
  - "Document type code table completeness" entry in [`docs/open-questions.md`](docs/open-questions.md) tracking the deliberate starter-set incompleteness
  - [`.claude/git-workflow.md`](.claude/git-workflow.md): full operational detail of the GitHub Flow + PR workflow (branch naming, per-PR steps, `gh` CLI usage, private-content scan timing, branch lifecycle); CLAUDE.md gets a short rules block pointing to it
  - Three new entries in [`.claude/known-pitfalls.md`](.claude/known-pitfalls.md): worktree branch names leaking into PRs, treating doc tensions as interpretive, and tagging a release before reality matches the claim
  - Historical-record header on [`.claude/pre-implementation-checklist.md`](.claude/pre-implementation-checklist.md) clarifying the doc is a snapshot of the gate state, not a live tracker

### Changed

- `docs/architecture.md` `domain` module description tightened to enumerate what's actually in the module and point to ADR-012 for value-class placement (was ambiguous about whether `CountryCode` and `DocumentType` lived in `domain` or `mrz-core`)
- Documentation alignment with shipped code (illustrative shapes only):
  - `docs/features/lookup-tables.md` — `byCategory` parameter type renamed `DocumentTypeCategory` → `DocumentCategory` to match the shipped enum in `domain`
  - `docs/features/mrz-data-model.md`, `docs/features/mrz-generation.md` — sealed-class variant references rewritten from nested `MrzDocument.TD3` form to top-level `TD3` form, matching the shipped data classes
  - `docs/features/mrz-data-model.md`, `docs/features/mrz-validation.md` — `ValidationResult` field renamed `errors` → `validationFailures` so the same conceptual list has the same name across `ResultMetadata` and `ValidationResult`

### Removed

- The `TestInfrastructureSmokeTest` placeholder in `mrz-core` (replaced by real tests as implementation slices landed)
- Empty `.gitkeep` placeholder files in source directories that now contain real Kotlin files

### Deferred (to future versions)

These are documented commitments that are explicitly *not* in this `[Unreleased]` snapshot. Each is tracked in [`docs/open-questions.md`](docs/open-questions.md) or in handoff watch items.

- Validator (`MrzValidator`, `ValidationResult`, `MrzCheckDigitMismatch`, `MrzUnknownCountryCode`, etc.)
- Generator (`MrzGenerator` and inverse round-trip property)
- Transliteration system (`TransliterationProfile`, `TransliterationProfileRegistry`)
- Auto-detect parser entry point (`MrzParser.parse(input)`)
- Other format parsers (TD1, TD2, MRV-A, MRV-B parser methods)
- Name field parsing (`primaryIdentifier` / `secondaryIdentifier` / `nameTruncated` extraction from raw name field)
- `CountryCode` value class + `CountryCodeTable`
- `MrzField` enum (lands with `MrzCheckDigitMismatch`)
- `MrzInvalidSexValue` validation error (parser currently silently defaults invalid sex chars to `UNSPECIFIED`)
- Warnings: `MrzExpiryDatePast`, `MrzExpiryDateImplausiblyFar`, `MrzNameTruncated`, `MrzPersonalNumberCheckDigitFiller`
- Document type code table population beyond the starter set
- `ResultMetadata.timing: TimingInfo?` field (no timing instrumentation yet)
- iOS targets, Android targets (waiting on Xcode install / 0.2.0 platform I/O work)
- Platform I/O modules (`mrz-camera-*`, `emrtd-nfc-*`, `mrz-camera-ui-*`)

[Unreleased]: https://github.com/askerasadov/tessera/compare/HEAD
