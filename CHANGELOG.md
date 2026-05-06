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
  - `CountryCode` value class wrapping a raw three-letter code with `entry`, `isRecognized`, `displayName`, `category` accessors
  - `CountryCodeTable` object with `lookup` / `all` / `byCategory`
  - `CountryCodeEntry` data class (code, displayName, category)
  - Starter set of 5 ISO 3166-1 alpha-3 state codes (`USA`, `GBR`, `DEU`, `FRA`, `JPN`); deliberately incomplete and tracked in [`docs/open-questions.md`](docs/open-questions.md) under "Country code table completeness"
- `mrz-core` module: TD3 parser
  - `MrzParser.parseTD3(input: String, referenceTime: Instant)` and `MrzParser.parseTD3(input: List<String>, referenceTime: Instant)` overloads
  - String input normalizes LF/CRLF/CR line endings, drops leading empty lines, trims trailing whitespace
  - Returns `ParseResult.Success` with structurally-valid input, `ParseResult.Failure(MrzInvalidLength(...))` for wrong shape, `ParseResult.Failure(MrzCharacterSetViolation(c, position))` for non-MRZ-alphabet characters
- `mrz-core` module: result types
  - `ParseResult` sealed class with `Success`, `PartialSuccess`, `Failure` variants
  - `ResultMetadata` aggregate (`readMethod`, `warnings`, `validationFailures`)
- `domain` module: validator-related taxonomy
  - `MrzField` enum (`ISSUING_STATE`, `DOCUMENT_NUMBER`, `NATIONALITY`, `DATE_OF_BIRTH`, `DATE_OF_EXPIRY`, `OPTIONAL_DATA`, `COMPOSITE`)
  - `MrzCheckDigitMismatch` validation error carrying `field`, `expected`, `observed`, `position`
  - `MrzInvalidSexValue` validation error carrying `observed`, `position`
  - `MrzUnknownDocumentTypeCode` warning carrying `rawCode`, `position`. Emitted by the validator when the document's `DocumentType` is not in `DocumentTypeCodeTable`. Categorical placement (warning, not validation failure) recorded in [ADR-013](docs/decisions/0013-recognition-failures-are-warnings.md): a recognition-table-derived check that reduces to "this code is not in our table" is a warning, because the SDK's tables are deliberately incomplete (Principle 1, Principle 4). Strict consumers who treat unrecognized codes as disqualifying read `result.warnings.isEmpty()` together with `result.validationFailures.isEmpty()`.
  - `MrzUnknownCountryCode` warning carrying `field: MrzField`, `rawCode`, `position`. Emitted by the validator when the document's `issuingState` or `nationality` `CountryCode` is not in `CountryCodeTable`. Same categorical placement as `MrzUnknownDocumentTypeCode` per [ADR-013](docs/decisions/0013-recognition-failures-are-warnings.md). The `field` discriminator distinguishes which of the two TD3 country-code positions emitted the warning (`MrzField.ISSUING_STATE` at TD3 position 2; `MrzField.NATIONALITY` at TD3 position 54).
- `mrz-core` module: validator (first slice)
  - `io.lightine.tessera.mrz.validation` subpackage (parallel to `checkdigit/`)
  - `ValidationResult(validationFailures, warnings)` aggregate (`passedChecks` deferred)
  - `MrzValidator.validate(document: MrzDocument): ValidationResult` — Layer 2 (per-field + composite check digits) and Layer 3 (sex value range) for TD3; TD1 returns an empty `ValidationResult` pending the TD1 parser slice
  - Parser wiring: `MrzParser.parseTD3` invokes `MrzValidator.validate(...)` after slicing fields and returns `ParseResult.PartialSuccess` (with failures populated in `ResultMetadata.validationFailures`) when any failure surfaces, otherwise `ParseResult.Success`
- `domain` module: expiry-date warning taxonomy
  - `MrzExpiryDatePast` warning carrying `expiryDate: LocalDate` and `referenceDate: LocalDate`
  - `MrzExpiryDateImplausiblyFar` warning carrying `expiryDate: LocalDate`, `referenceDate: LocalDate`, and `thresholdYears: Int`
- `mrz-core` module: expiry-date warnings (first warning slice)
  - `MrzValidator.validate(document, referenceTime: Instant = Clock.System.now())` overload — `referenceTime` parameter added with a default; non-breaking
  - TD3 expiry warnings emitted when `commonFields.dateOfExpiry.computedDate` is non-null: past relative to `referenceTime` → `MrzExpiryDatePast`; more than 10 years after `referenceTime` → `MrzExpiryDateImplausiblyFar`. The 10-year threshold is a private constant for now; configurability is tracked in [`docs/open-questions.md`](docs/open-questions.md) "Validator options (configurable thresholds)"
  - `MrzParser.parseTD3` threads its own `referenceTime` through to `MrzValidator.validate(...)`, fixing a latent inconsistency where the validator previously fell back to `Clock.System.now()` while the parser computed the expiry's `computedDate` against the caller's `referenceTime`
  - Warnings populate `ResultMetadata.warnings` independently of the Success/PartialSuccess decision — a result with warnings but no failures is `Success`
- `domain` module: date-in-calendar validation taxonomy
  - `MrzDateNotInCalendar` validation error carrying `field: MrzField`, `rawYear`, `rawMonth`, `rawDay`, and `position`
- `mrz-core` module: date-in-calendar validation (TD3, both birth and expiry)
  - `MrzDate.componentsFormCalendarDate: Boolean?` tri-state signal added with a default of `null`; populated by `parseBirth` and `parseExpiry`. `null` when raw components did not parse as 2-digit numerics, `true` when components form a real `LocalDate` for at least one candidate century (covers successful inference and out-of-window calendar-valid dates), `false` when no candidate year forms a calendar date. The default keeps existing `MrzDate(...)` constructor call sites compiling unchanged (Principle 9). One nuance: like any `data class` property addition, `equals`/`hashCode`/`copy`/`componentN()` now include the new field — code that manually constructs an `MrzDate` with all other fields populated and compares it to a parser-produced instance will find them unequal where they would have been equal before (manual default = `null`, parser-set = `true`). No call site in the project's own code hits this path; flagged here for completeness
  - `MrzValidator.validateTD3` emits `MrzDateNotInCalendar` for `dateOfBirth` (position offset 13 on line 2) and `dateOfExpiry` (position offset 21) when `componentsFormCalendarDate == false`. Calendar-valid dates that the parser rejects via the inference window (e.g., expiry > 50 years out) do not produce this failure — the date IS in the calendar, just outside the heuristic
- `domain` module: birth-date warning taxonomy
  - `MrzBirthDateImplausiblyOld` warning carrying `rawYear`, `rawMonth`, `rawDay`, `referenceDate: LocalDate`, and `thresholdYears: Int`
- `mrz-core` module: birth-date warning (TD3 birth)
  - `MrzDate.componentsExceedBirthAgeLimit: Boolean?` tri-state signal added with a default of `null`; populated only by `parseBirth`. `true` when the parser falls to `RAW_ONLY` because every calendar-valid past candidate exceeds the parser's age cap (`MAX_PLAUSIBLE_AGE_YEARS = 130`); `false` when the parser succeeded or when no past calendar-valid candidate exists; `null` for `parseExpiry`-produced dates, direct-construction defaults, non-numeric components, or when no candidate forms a calendar date. Same data-class equality nuance as `componentsFormCalendarDate` — code that manually constructs an `MrzDate` with all other fields populated and compares it to a parser-produced instance will find them unequal where they would have been equal before
  - `MrzValidator.validateTD3` emits `MrzBirthDateImplausiblyOld` for `dateOfBirth` when `componentsExceedBirthAgeLimit == true`. Threshold reported on the warning is sourced from `MrzDate.MAX_PLAUSIBLE_AGE_YEARS` (visibility lifted from `private` to `internal` so the validator can read the same constant the parser uses for inference). Under current-era reference times (year ≤ ~2130) the cap is unreachable in practice; the warning matters for replay scenarios, audit pipelines, and far-future reference times that consumers may pass explicitly. Closes the documented commitment in `mrz-validation.md` "Date Range Conventions" that the validator surfaces an implausibly-old birth signal at a 130-year threshold
- Build infrastructure
  - `kotlinx-datetime 0.6.1` declared as `api` dependency in `mrz-core` (transitively exposes `LocalDate`)
  - `kotlinx-datetime 0.6.1` declared as `api` dependency in `domain` (first date-bearing types in `domain` — the expiry warnings — carry `LocalDate`)
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
- `CommonFields` gains `rawSex: Char` so the verbatim sex character is preserved on the model even when `Sex.UNSPECIFIED` is the mapped value (Principles 1 + 5). The validator reads `rawSex` to decide `MrzInvalidSexValue`.
- `docs/features/mrz-validation.md`: new "Status of Implementation" section enumerates which capabilities ship in this snapshot vs. which are documented but deferred. Each deferral cross-references an entry in `docs/open-questions.md`.
- `docs/open-questions.md`: new entries for the validator deferrals (string-input overloads, `passedChecks` shape, `MrzUnknownCountryCode`, `MrzUnknownDocumentTypeCode`, `MrzDateNotInCalendar`, expiry warnings, TD1 validator path) and the canonical sex-value set per ICAO Doc 9303 primary source.
- `docs/features/mrz-validation.md`: status row for expiry warnings flipped from Deferred → Implemented; new prose paragraph documenting the layered limitation that `MrzExpiryDateImplausiblyFar` can fire only within the (ref+10y, ref+50y] window because `MrzDate.parseExpiry` rejects expiries beyond +50y as `RAW_ONLY`.
- `docs/open-questions.md`: "Expiry-date warnings" entry marked Resolved; new entry "Validator options (configurable thresholds)" tracking the deferral of a `ValidationOptions`-style configuration surface.
- `docs/features/mrz-validation.md`: status row for date-in-calendar flipped from Deferred → Implemented; new prose paragraph documenting that the dispatch is signal-driven from `MrzDate.componentsFormCalendarDate` so non-numeric components (Layer-1 territory) and calendar-valid out-of-window dates do not produce the failure.
- `docs/features/mrz-data-model.md`: `MrzDate` field listing extended with `componentsFormCalendarDate`, including the semantics of each tri-state value.
- `docs/open-questions.md`: "Date-in-calendar validation (`MrzDateNotInCalendar`)" entry marked Resolved with cross-references to the data-model and validation feature docs.
- `docs/features/mrz-validation.md`: status table gains a row for `MrzBirthDateImplausiblyOld` (TD3 birth, threshold 130 years matching the parser's `MAX_PLAUSIBLE_AGE_YEARS`); new prose paragraph documenting the signal-driven dispatch from `MrzDate.componentsExceedBirthAgeLimit` and the practical reachability under current-era reference times. "Date Range Conventions" bullet for date-of-birth rewritten to describe what the validator actually surfaces (was a documented commitment that the validator did not yet fulfill — pre-existing doc drift identified in the previous handoff and resolved by this slice).
- `docs/features/mrz-data-model.md`: `MrzDate` field listing extended with `componentsExceedBirthAgeLimit`, including the semantics of each tri-state value.
- `docs/features/mrz-error-taxonomy.md`: `MrzBirthDateImplausiblyOld` added to the warnings catalog.
- `docs/features/mrz-error-taxonomy.md`: `MrzUnknownCountryCode` and `MrzUnknownDocumentTypeCode` moved from "Validation Failures" to "Warnings" in the Representative Examples section, aligning with the existing prose commitments in `lookup-tables.md` and `mrz-validation.md` ("Recognition vs Conformance"). Naming Conventions section updated to use `MrzInvalidSexValue` as the validation-error example instead of `MrzUnknownCountryCode`. Resolves a pre-existing internal contradiction in the doc set; the placement under "Validation Failures" was the outlier. Reasoning recorded in [ADR-013](docs/decisions/0013-recognition-failures-are-warnings.md).
- `docs/features/mrz-validation.md`: status row for document type code recognition flipped from Deferred → Implemented (TD3); status row for country code recognition annotated with the same prospective placement (warning) per ADR-013; new prose paragraph documents the unconditional check, the `rawCode`/position payload, and the strict-consumer pattern.
- `docs/decisions/0013-recognition-failures-are-warnings.md`: new ADR formalizing the principle-grounded reasoning. Recognition-table-derived checks that reduce to "this code is not in our table" surface as warnings, not validation failures. Generalizes to `MrzUnknownCountryCode` and any analogous future check.
- `docs/open-questions.md`: "Document type code recognition validation (`MrzUnknownDocumentTypeCode`)" entry marked Resolved with a cross-reference to ADR-013. The separate "Document type code table completeness" entry remains open (the starter set is still the deliberate-incomplete one).
- `mrz-core` and `domain` Gradle builds now enable Kotlin's `explicitApi()` strict mode. Existing code is already 100% compliant — every top-level and member declaration has an explicit visibility modifier, and the build passes without changes. The mode is a forward-looking enforcement gate: future declarations missing `public` / `internal` / `private` (or missing return types on public API functions) now fail compilation rather than silently defaulting to public. Resolves audit item 2 from the 2026-05-04 alignment recap (lock down the public API surface before more slices accumulate, and especially before `CountryCode` doubles `mrz-core`'s public footprint). The other modules (`emrtd-core`, `telemetry`, `logging`) are empty placeholders today and not in scope; when their first source file lands, `explicitApi()` should be added as part of that slice.
- `CommonFields.issuingState` and `CommonFields.nationality` types changed from `String` to `CountryCode` (the recognition-bearing value class). Pre-0.1.0, no backcompat impact. The change brings recognition state onto the model directly, parallel to how `documentType: DocumentType` already exposes `isRecognized`. `MrzParser.parseTD3` constructs the `CountryCode` instances; `MrzValidator.validateTD3` consults `isRecognized` to dispatch the `MrzUnknownCountryCode` warning.
- `MrzField` enum extended with `ISSUING_STATE` and `NATIONALITY` variants so `MrzUnknownCountryCode` can identify which of the two TD3 country-code positions surfaced the warning. The enum's order is now field-position order (line 1 → line 2), making it easier to reason about. No existing call site is sensitive to the ordering.
- Session handoff filename convention extended from `SESSION-HANDOFF-YYYY-MM-DD-<slug>.md` to `SESSION-HANDOFF-YYYY-MM-DD-HHMM-<slug>.md`, where `HHMM` is the UTC time as four digits with no separator. The motivation: when multiple handoffs land on the same calendar day (three on 2026-05-05), the slug-only convention fell back to mtime as the same-day tiebreaker, but mtime is unreliable across `git clone`, `rsync`, archive extraction, and file syncs. With the time component in the filename, `ls -1 SESSION-HANDOFF-*.md | sort -r | head -1` returns the canonical latest deterministically. The slug stays so the directory listing is still self-documenting at a glance. Updated `CLAUDE.md` ("What to Do First" + "Session Discipline") and `.claude/session-handoff-template.md` ("Where to Put the Handoff"). Legacy handoff files (date-only and date+slug) are not bulk-renamed; the new convention applies going forward and within-date form mixing is not expected.

### Removed

- The `TestInfrastructureSmokeTest` placeholder in `mrz-core` (replaced by real tests as implementation slices landed)
- Empty `.gitkeep` placeholder files in source directories that now contain real Kotlin files

### Deferred (to future versions)

These are documented commitments that are explicitly *not* in this `[Unreleased]` snapshot. Each is tracked in [`docs/open-questions.md`](docs/open-questions.md) or in handoff watch items.

- Validator standalone string-input overloads (`MrzValidator.validate(input: String / List<String> / String, format)`); current slice ships `validate(MrzDocument)` only
- `ValidationResult.passedChecks` transparency surface (current `ValidationResult` exposes `validationFailures` + `warnings` only)
- TD1 validator path (validator currently returns an empty `ValidationResult` for TD1 inputs; lands with the TD1 parser slice)
- Validator options surface (`ValidationOptions`-style configurable thresholds); current slice ships `MrzExpiryDateImplausiblyFar`'s 10-year threshold as a private constant
- Generator (`MrzGenerator` and inverse round-trip property)
- Transliteration system (`TransliterationProfile`, `TransliterationProfileRegistry`)
- Auto-detect parser entry point (`MrzParser.parse(input)`)
- Other format parsers (TD1, TD2, MRV-A, MRV-B parser methods)
- Name field parsing (`primaryIdentifier` / `secondaryIdentifier` / `nameTruncated` extraction from raw name field)
- Warnings: `MrzNameTruncated`, `MrzPersonalNumberCheckDigitFiller`
- Document type code table population beyond the starter set
- Country code table population beyond the starter set
- `ResultMetadata.timing: TimingInfo?` field (no timing instrumentation yet)
- iOS targets, Android targets (waiting on Xcode install / 0.2.0 platform I/O work)
- Platform I/O modules (`mrz-camera-*`, `emrtd-nfc-*`, `mrz-camera-ui-*`)

[Unreleased]: https://github.com/askerasadov/tessera/compare/HEAD
