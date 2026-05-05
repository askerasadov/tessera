# Open Questions

This document tracks decisions that have been deliberately deferred — to implementation time, to a future release, or to a moment when more information is available. The purpose is to ensure no deferred item is forgotten between design and implementation.

This document is living. Items are added when a decision is deferred during design. Items are removed when the decision is made and recorded in the appropriate document (a feature doc, an ADR, or scope.md). An entry that lingers without progress is itself a signal that the question may need attention.

Each entry includes a short description of the question, where it was deferred from, and what kind of resolution it requires.

---

## Deferred to Implementation Time

These questions are not answerable from design alone. They will be settled when implementation begins, often after experimentation or measurement.

### Public API exact names and signatures

The illustrative Kotlin-flavored shapes in feature documents (e.g., `MrzParser.parse(input)`, `MrzGenerator.generateTD3(...)`) describe the intended contracts but do not lock the exact class names, method names, parameter ordering, or visibility modifiers. The final shapes are decided at implementation time, recorded as feature documentation is updated.

**Source:** `mrz-parsing.md`, `mrz-generation.md`, `mrz-validation.md`, `lookup-tables.md`, `transliteration.md`

**Resolution:** Update each affected feature document with the final API shape once implementation lands.

### Code style tooling

Code style conventions (formatter, linter, configuration choices) are deferred until implementation begins. The current commitment is that code follows the idiomatic conventions of each target language; the specific tools are chosen and documented when first used.

**Source:** `conventions.md` ("Code Style" section)

**Resolution:** Resolved — Kotlin code is formatted and linted via Spotless (applied at the project root) with a ktlint backend, configured against `.editorconfig` and the Kotlin official style. See the "Code Style" section of `conventions.md` for details.

### Project root namespace

The root namespace for package paths is **`io.lightine.tessera`**. The `io.` prefix follows the modern convention for developer tools and SDKs; `lightine` is the brand segment; `tessera` is the project segment.

**Source:** `architecture.md`, `conventions.md`

**Resolution:** Resolved — root namespace is `io.lightine.tessera`. Sub-package structure (e.g., `io.lightine.tessera.mrz.parsing`) emerges as code is written.

### Specific date inference thresholds

The validator uses specific thresholds for date plausibility checks (130 years for date of birth, etc.). These are documented as defaults and configurable; the actual default values may be tuned during implementation based on testing against real-world data.

**Source:** `mrz-validation.md` ("Date Range Conventions")

**Resolution:** Confirm or adjust thresholds during implementation; document the chosen values in `mrz-validation.md`.

### Validator string-input and explicit-format overloads

`mrz-validation.md` documents `MrzValidator.validate(input: String)`, `validate(input: List<String>)`, and the corresponding overloads with an explicit `MrzFormat`. The first validator slice ships only `validate(document: MrzDocument)`. The string-input path is the standalone validation surface for consumers who want to validate previously-extracted data without re-parsing; it is not blocking the parser-internal validation path.

**Source:** First validator implementation slice; aligns with `mrz-validation.md` "Status of Implementation".

**Resolution:** Add string-input overloads in a follow-up slice. They should reuse the same per-format validators that `validate(document)` dispatches to, so a check digit failure detected by the standalone string path produces the same typed error as one detected by the parser-internal path.

### `ValidationResult.passedChecks` shape

`mrz-validation.md` describes `passedChecks` as a transparency surface — "the validators that ran and passed (exposed for transparency; consumers can confirm what was actually verified, not just what failed)." The first validator slice ships `ValidationResult` with `validationFailures` and `warnings` only. Committing to a shape for `passedChecks` (typed enum/sealed list, plain string list, or richer record) before the validator catalog is broader would be a guess about consumer needs (Principle 2).

**Source:** First validator implementation slice; aligns with `mrz-validation.md` "Status of Implementation".

**Resolution:** Decide the shape when more semantic checks land and the catalog is broader. Add `passedChecks` to `ValidationResult` with a default value to keep the addition non-breaking (Principle 9).

### Country code recognition validation (`MrzUnknownCountryCode`)

`mrz-error-taxonomy.md` lists `MrzUnknownCountryCode` as a representative validation failure: the issuing state or nationality code is not in the recognized lookup tables. The first validator slice does not produce this failure because the SDK does not yet have a `CountryCode` value class or `CountryCodeTable`.

**Source:** First validator implementation slice; aligns with `lookup-tables.md` ("Initial Country Code Coverage").

**Resolution:** Add the country-code recognition check together with the `CountryCode` value class + `CountryCodeTable` slice. Same ADR-012 pattern as `DocumentType`.

### Document type code recognition validation (`MrzUnknownDocumentTypeCode`)

`mrz-error-taxonomy.md` lists `MrzUnknownDocumentTypeCode` as a representative validation failure. The `DocumentType` value class and `DocumentTypeCodeTable` already exist (with a starter set), so the recognition signal is available via `DocumentType.isRecognized`. The first validator slice does not produce this failure to keep the slice focused on closing the check-digit translation-owed loop.

**Source:** First validator implementation slice.

**Resolution:** Add the document-type recognition check in a focused follow-up slice. Decide whether a starter-set unrecognized code should be a failure or a warning given the deliberate incompleteness of the table (`docs/open-questions.md` "Document type code table completeness").

### Date-in-calendar validation (`MrzDateNotInCalendar`)

`mrz-error-taxonomy.md` lists `MrzDateNotInCalendar` as a representative validation failure: a date is structurally well-formed (six digits) but does not represent a real calendar date (e.g., February 30). The current parser already tolerates this — `MrzDate.parseBirth` and `parseExpiry` return `MrzDateInferenceMethod.RAW_ONLY` when the components do not form a valid date — but the validator does not surface a failure for it.

**Source:** First validator implementation slice; aligns with `mrz-validation.md` "Layer 3 — Semantic" and `mrz-error-taxonomy.md`.

**Resolution:** Resolved — implemented for TD3 in `MrzValidator` for both `dateOfBirth` and `dateOfExpiry`. The dispatch is signal-driven from a new tri-state property on the model: `MrzDate.componentsFormCalendarDate: Boolean?`. The original `RAW_ONLY` enum value collapsed three distinct failure modes; the new property disambiguates them so the validator can emit `MrzDateNotInCalendar` only for the "components numeric but no calendar date" case, leaving "non-numeric components" (Layer-1 territory) and "calendar-valid but outside the parser's inference window" (a date that IS in the calendar) untouched. See `docs/features/mrz-data-model.md` "MrzDate" and `docs/features/mrz-validation.md` "Status of Implementation."

### Expiry-date warnings (`MrzExpiryDatePast`, `MrzExpiryDateImplausiblyFar`)

`mrz-error-taxonomy.md` lists `MrzExpiryDatePast` and `MrzExpiryDateImplausiblyFar` as representative warnings. The first validator slice produces no warnings (`ValidationResult.warnings` is always empty for now); these are the natural first warning slice.

**Source:** First validator implementation slice; aligns with `mrz-validation.md` "Date Range Conventions" and `mrz-error-taxonomy.md`.

**Resolution:** Resolved — both warnings are implemented for TD3 in `MrzValidator`. `MrzValidator.validate(...)` now accepts an explicit `referenceTime` (defaulting to `Clock.System.now()`); `MrzParser.parseTD3` threads its own `referenceTime` through. `MrzExpiryDateImplausiblyFar` carries `thresholdYears` (defaulting to 10) on the warning itself. Configurability of the threshold is its own deferred question — see "Validator options (configurable thresholds)" below.

### Validator options (configurable thresholds)

`mrz-validation.md` "Date Range Conventions" commits to thresholds being "configurable through the validator's options, with the documented defaults applied when no configuration is provided." The first warning slice ships the implausibly-far threshold as a private constant in `MrzValidator` (10 years, matching `mrz-error-taxonomy.md`). Building a `ValidationOptions`-style surface now would be a guess about which other thresholds eventually need configuring (Principle 11 — internal first, promote when justified).

**Source:** First warning implementation slice; aligns with `mrz-validation.md` "Date Range Conventions".

**Resolution:** When a second configurable threshold lands (likely the date-of-birth `MAX_PLAUSIBLE_AGE_YEARS` cap, or expiry-window thresholds revisited under real-world data), introduce a `ValidationOptions` value class with named, defaulted properties and a single `MrzValidator.validate(document, referenceTime, options)` overload. Keep the defaults exactly matching the current private constants so the addition is non-breaking (Principle 9).

### TD1 validator path

The first validator slice handles only TD3. For TD1 inputs, `MrzValidator.validate(...)` returns an empty `ValidationResult` (no failures, no warnings) because TD1 has no parser yet, so there is no integration test path that would exercise a TD1 validator end-to-end. Implementing TD1's composite check digit formula without a TD1 parser to drive it would produce code that compiles and runs but is not meaningfully tested against real parsed input.

**Source:** First validator implementation slice; aligns with the TD1 data-class-only state (PR #1 slice 8).

**Resolution:** Land the TD1 validator path together with the TD1 parser slice, so the two are tested as one end-to-end flow.

---

## Deferred to a Future Release

These questions concern functionality that is intentionally not in the current scope. They are tracked here so they are not forgotten when their release approaches.

### Lenient and tolerant parsing modes

The parser currently operates in strict mode only. Lenient mode (tolerating real-world deviations such as extra whitespace) and tolerant mode (recovering from OCR confusions using check-digit-guided disambiguation) are intentionally deferred. They are additive capabilities; the strict-only API does not constrain their later addition.

**Source:** `mrz-parsing.md` ("Strictness")

**Resolution:** Design and implement these modes in a future release when camera reading is added (lenient mode is most relevant when input comes from OCR).

### Sex field encoding choice (`<` vs `X`)

The generator currently encodes `Sex.UNSPECIFIED` as the filler character `<` by default. Future configuration may allow choosing `X` explicitly. The current decision is made; the configurability is deferred.

**Source:** `mrz-generation.md` ("Edge Cases Worth Calling Out")

**Resolution:** Add configuration option in a future release if consumer demand justifies it.

### Profile inheritance for transliteration

The initial transliteration system does not support profiles inheriting from each other. A "based on ICAO default with overrides" pattern is a possible future enhancement.

**Source:** `transliteration.md` ("Edge Cases Worth Calling Out")

**Resolution:** Add inheritance mechanism in a future release if multiple profiles share substantial common content.

### Multiple profiles per state

A state may have multiple transliteration conventions (different document types, different time periods). The current model represents this as multiple profiles with distinct identifiers (e.g., `XYZ-CURRENT`, `XYZ-LEGACY`). A more structured approach may be added later if needed.

**Source:** `transliteration.md` ("Edge Cases Worth Calling Out")

**Resolution:** Revisit if real-world use cases require structured per-context profile selection.

### MIXED read method semantics

The `ReadMethod.MIXED` enum value represents results that combine MRZ from camera with chip data from NFC. This becomes meaningful only when both reading paths are implemented (release 0.6.0 and later).

**Source:** `mrz-data-model.md` (`ReadMethod` enum), `mrz-error-taxonomy.md` (chip/camera mismatch warning example)

**Resolution:** Define exact semantics when NFC reading is implemented and combined-result use cases are concrete.

### Image and capture metadata exposure

When pre-captured image reading and live camera reading are implemented, the SDK has access to metadata about the image or capture: timestamps (EXIF `DateTimeOriginal`, capture time), device identifiers (camera make and model), GPS coordinates if present in EXIF, indicators of editing or screenshot origin, and similar. Exposing this metadata to consumers would help them assess risks the SDK currently surfaces only as "this came from a pre-captured image, here's the risk profile."

This aligns with Principle 5 (Transparency — if we have the data, we expose it) and Principle 1 (Reader, not oracle — consumer interprets the metadata). It also has real complications: metadata can be stripped, fabricated, or simply absent; some fields are PII (notably GPS coordinates) and warrant careful handling; reliability documentation is essential or consumers may over-trust untrustworthy data.

**Source:** Design conversation about how consumers can better assess pre-captured image risk; aligns with `reading-risks.md`.

**Resolution:** Design when pre-captured image reading work begins (release 0.3.0 target). Settle which fields are exposed, how reliability is documented, how PII is handled, and whether the same approach extends to live camera capture metadata.

### Security review pass before public release

Before the 1.0.0 public release, perform a systematic security review pass. The pass should reference the "Areas for Further Analysis" section of `reading-risks.md` and confirm which theoretical concerns are real, which are moot, and which require mitigation. Items confirmed as real either get fixed or get documented in `reading-risks.md` so consumers can account for them. Items confirmed as moot get marked as such so future contributors do not re-litigate them.

The pass is most usefully scheduled after release 0.6.0 (NFC chip reading lands — significant cryptographic surface) but before 1.0.0 (the moment public stability commitments take effect).

**Source:** `reading-risks.md` ("Areas for Further Analysis"); design conversation about theoretical risk handling.

**Resolution:** Schedule and perform the review pass before tagging 1.0.0. Update `reading-risks.md` and other affected documents with the findings.

### Android target configuration on core modules

Core modules are scaffolded with the JVM target only. The Android target is intentionally deferred until 0.2.0 work begins, when the first Android-touching module (`mrz-camera-android`) is introduced and AGP needs to be added to the build anyway. Adding `androidTarget()` to the pure-logic core modules earlier would buy only theoretical insurance against Android-incompatible APIs sneaking into `commonMain`, at the cost of pulling AGP and its version-coupling constraints into the build before they earn their keep (Principle 2: the option that assumes less wins; Principle 11: don't promote infrastructure before it's justified). The Android SDK is already installed on the development machine; the deferral is by intent, not by tooling gap.

**Source:** Pre-implementation scaffolding session; aligns with Principles 2 and 11.

**Resolution:** When 0.2.0 platform I/O work begins, add `androidTarget()` to each core module's `kotlin { ... }` block alongside introducing AGP and `mrz-camera-android`. Verify common code compiles for Android. Remove this entry.

### Platform I/O and UI module scaffolding

The pre-implementation checklist names `mrz-camera-{platform}`, `emrtd-nfc-{platform}`, and `mrz-camera-ui-{platform}` modules as scaffold targets. They are not scaffolded in 0.1.0 because each requires its corresponding platform toolchain (AGP for Android variants, Xcode for iOS variants) and there is no implementation in 0.1.0 that would exercise an empty-shell module. Empty platform modules add build configuration that has to be maintained without delivering any value until the corresponding feature work begins.

**Source:** Pre-implementation scaffolding session; aligns with `architecture.md` ("as appropriate" wording in the checklist) and Principle 11.

**Resolution:** Scaffold each platform I/O and UI module together with its first implementation work, on its own release schedule per the roadmap in `scope.md` (camera I/O at 0.2.0, NFC I/O at 0.6.0, UI per platform alongside the corresponding I/O). Remove this entry once all named modules are either scaffolded or struck from the architecture.

---

## Deferred to a Future Document

These items are referenced from existing documents but their full content lives in documents not yet written.

### Reading risks documentation

Each reading method (live camera, pre-captured image, manual entry, NFC chip, backend string input) has a different risk profile — what it establishes about the data, what it does not, what classes of attacks or errors are possible, what additional verification consumers might want to layer on top. This documentation lives in its own file (`reading-risks.md`).

**Source:** `scope.md` ("Risk Documentation")

**Resolution:** Resolved — `reading-risks.md` exists.

### Glossary

Terms used throughout the documentation (MRZ, BAC, PACE, eMRTD, TD1, TD2, TD3, MRV-A, MRV-B, SOD, LDS, etc.) would benefit from a single reference. Currently, each term is explained in context where it first appears.

**Source:** Implicit gap; not yet referenced from any doc.

**Resolution:** Resolved — `glossary.md` exists.

### Architecture Decision Records

Several significant decisions made during design (Kotlin Multiplatform choice, native UI per platform, reader-not-oracle as foundational, no verification hooks in initial release, Position A backward compatibility from 0.x, etc.) deserve formal ADR documentation for future contributors.

**Source:** Implicit; conventions document the ADR format but no ADRs exist yet.

**Resolution:** Resolved — eleven ADRs exist in `docs/decisions/`. Additional ADRs may be added in the future as new significant decisions are made.

### README

A project-front-door document does not yet exist. It will be added when the project moves from internal to public visibility.

**Source:** Implicit; not yet referenced.

**Resolution:** Resolved — `README.md` exists at project root. May be revised before public release as the project's identity finalizes.

### CLAUDE.md (AI handoff document)

A document specifically structured to help AI assistants (Claude Code in particular) work effectively with this project. It would point at the right docs in the right order, capture project-specific context, and codify the working patterns established during design.

**Source:** Implicit; design conversation referenced this as a planned artifact.

**Resolution:** Resolved — `CLAUDE.md` exists at project root, supported by working notes in `.claude/`.

---

## Deferred Pending External Information

These questions cannot be settled by us alone — they depend on documentation, decisions, or developments outside the project.

### Specific document type implementations

Some document types are in scope but their specific format details require documentation that may not be currently public. The architecture supports them; implementation is added when documentation becomes available.

**Source:** `scope.md` ("Specific Document Implementations")

**Resolution:** Implement each as documentation becomes available.

### Sex value canonical set per ICAO Doc 9303

`mrz-error-taxonomy.md` lists the valid sex characters as `M`, `F`, `<`, or `X`. The first validator slice uses this set as the allowed characters for `MrzInvalidSexValue`. ICAO Doc 9303 Part 4 §4.1 historically lists `M`, `F`, `<`; later guidance is reported to permit `X` for non-binary documents, and some issuing states use it. The project does not currently have an authoritative copy of the relevant ICAO publication at hand to confirm which set is canonical.

**Source:** First validator implementation slice; aligns with `mrz-error-taxonomy.md` representative-examples list.

**Resolution:** Confirm the canonical valid set against ICAO Doc 9303 primary source. If `X` is canonical, no code change is needed (the validator already permits it). If `X` is not canonical, narrow the validator's set, update `mrz-error-taxonomy.md`, and add a test for the change.

### Document type code table completeness

The `DocumentTypeCodeTable` in `mrz-core` ships with a deliberate starter set of well-known codes (`P`, `V`, `I`, `PP`, `PD`, `PS`) — not the complete enumeration committed to in `docs/features/lookup-tables.md` ("Initial Document Type Code Coverage"). The complete list comes from ICAO Doc 9303 Part 3 Section 4, which the project does not currently have at hand.

The architectural pattern (recognition-bearing value class living next to its lookup table per ADR-012) is in place; the data is intentionally partial. The starter set is documented as such in `DocumentTypeCodeTable.kt` (file-level comment), and the table is structured so that adding entries is a non-breaking change (per `lookup-tables.md` "Updating the Tables"). A test (`DocumentTypeCodeTableTest.by_category_residence_permit_returns_empty_list_in_starter_set`) locks the current coverage so any expansion surfaces explicitly.

**Source:** First implementation slice for `DocumentType` (2026-05-04 session); aligns with `lookup-tables.md` coverage commitment.

**Resolution:** Populate the table from current ICAO Doc 9303 Part 3 Section 4 when an authoritative copy of the publication is available. Update tests to match the full set. Remove this entry once the table matches the spec.

### Driver's license format choice (mDoc vs proprietary)

When driver's license NFC reading is added in a future release, the choice between standard mDoc-compliant licenses (ISO 18013-5) and proprietary national formats depends on which markets the project prioritizes.

**Source:** `scope.md` ("Beyond 1.0")

**Resolution:** Decide when driver's license NFC work begins, based on consumer needs at that time.

### Trust anchor source for chip signature verification

Cryptographic verification of NFC chip signatures requires trust anchors (typically Country Signing Certificate Authority certificates, distributed via the ICAO Public Key Directory or similar). The choice of trust anchor source is its own design problem, deferred until chip signature verification is on the active roadmap.

**Source:** `scope.md` ("Beyond 1.0")

**Resolution:** Design when chip signature verification is added.

### Distribution channels (Maven Central, CocoaPods, SPM)

The choice of artifact distribution channels for each target platform is not yet decided. Current focus is on producing usable artifacts; publication mechanics will be settled before public release.

**Source:** Implicit; not yet referenced.

**Resolution:** Decide and document before public release.

### LICENSE file at project root

ADR-010 commits the project to the Apache License 2.0 at public release. The `LICENSE` file at project root contains the full Apache 2.0 license text with copyright attribution to "Asker Asadov (Lightine)".

**Source:** ADR-010 (Apache 2.0 license at public release).

**Resolution:** Resolved — `LICENSE` file exists at project root with Apache 2.0 text and proper attribution. A `NOTICE` file is not needed yet because no third-party content currently requires attribution; create one if/when third-party content with NOTICE requirements is incorporated.

### Git platform choice

The project will use Git for version control (decided). The hosting platform is not finalized; GitHub is the leading candidate but other options (GitLab, Codeberg, self-hosted) remain possible.

**Source:** Design conversation about implementation tooling.

**Resolution:** Resolved — GitHub is the chosen hosting platform. CI workflows, issue templates, and any other platform-specific configuration are added in a follow-up before the first public push.

### Project name and brand attribution

The project name is **Tessera** — Latin for an inscribed token used in the Roman world as identification, a pass, or a token of recognition. The semantic fit captures what the SDK does: structured, identifiable data extracted from documents.

The brand attribution is **"Asker Asadov (Lightine)"** — personal name as the legal copyright holder, with Lightine as the project's brand. This balances legal clarity (the rights-holder is a real legal entity, the individual) with brand visibility (Lightine remains visible as the project's umbrella).

**Source:** Design conversation about project identity.

**Resolution:** Resolved — project name is Tessera, attribution is "Asker Asadov (Lightine)" in the LICENSE file.

### Local regulatory considerations for the project's author

The project's author is in a jurisdiction whose regulatory frameworks for open source software, contributor patent grants, government data handling, and conflict-of-interest rules differ from US/EU contexts and are evolving. Specific legal review may be warranted before public release, particularly if the SDK is later deployed in any government context.

**Source:** Design conversation about author location and local context.

**Resolution:** Consult applicable local legal guidance before public release. Apache 2.0 with explicit patent grant (ADR-010) is a defensive choice that helps where local frameworks are less codified, but does not substitute for legal review.

### iOS target configuration on core modules

Core modules (`mrz-core`, `emrtd-core`, `domain`, `telemetry`, `logging`) are scaffolded with the JVM target only. Configuring the iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) requires Xcode, which is not installed on the development machine where scaffolding was performed. There is no design decision to make — the targets are committed in `architecture.md` and ADR-002. The deferral is purely about toolchain availability.

**Source:** Pre-implementation scaffolding session; depends on Xcode install.

**Resolution:** When Xcode is installed (any version supporting the project's iOS 15.0 minimum), add the three iOS targets to each core module's `kotlin { ... }` block, verify common code compiles on Konan, and remove this entry.

---

## Future Project Toolkit

This is not a project-specific item but is recorded here so it is not lost. It can be moved to a separate document later if it grows.

### Reusable patterns and document templates for future projects

The patterns established during this project's design — dispute-driven discussion, principles-first design, the specific document templates (ADR format, feature doc structure, etc.) — are not specific to this SDK. They could be useful for future projects undertaken by the same author. Extracting them into a reusable toolkit (outside this project's repository, in a personal `~/code/.shared-context/` or similar) would let future projects start with the same disciplines without re-deriving them.

**Source:** Design conversation about future-project considerations.

**Resolution:** Extract patterns to a separate personal toolkit at a future point. Not blocking this project's progress; tracked here so it does not get lost.

---

## Future Improvements to Consider

These are not deferred decisions and not blocking. They are improvements to the project's documentation system or process that may be worth making *if certain conditions arise*. Each entry includes the trigger that would justify revisiting.

The list is intentionally short. Premature improvements are noise; trigger-based items get acted on when relevant rather than gathering dust.

### Document Evolution section in conventions.md

Add explicit guidance on how documents evolve: when feature docs get rewritten vs. extended, when sections move to their own docs, when ADRs get superseded vs. updated, how to handle obsolete content.

**Trigger:** When documentation patterns start drifting noticeably between docs, or when a contributor asks "how should this document change?"

### Lessons-learned log

Add a `LESSONS.md` or `RETROSPECTIVE.md` capturing what went well and what didn't, after each release or milestone. Useful for long-term project health.

**Trigger:** After the first internal release (0.1.0) ships, when there is actually a milestone to retrospect on.

### Code precedent examples

Once implementation has produced idiomatic code in the project, consider whether to extract small example snippets into the documentation as "this is what a parser implementation in this project looks like." Not pre-written — emerges from real first implementations.

**Trigger:** After 0.1.0 lands, if Claude Code consistently produces non-idiomatic code that requires correction.

### CONTRIBUTING.md at project root

GitHub recognizes a top-level `CONTRIBUTING.md` and surfaces it on PR creation. The current `docs/conventions.md` covers what a CONTRIBUTING.md would cover. A small top-level file pointing to conventions.md may be useful when the project goes public on GitHub.

**Trigger:** Before the first public push to GitHub or equivalent.

### CHANGELOG.md initial entry

The project commits to Keep a Changelog format (see `docs/versioning.md`). The actual `CHANGELOG.md` file does not yet exist. It will be created with the first internal release entry.

**Trigger:** Before tagging 0.1.0.

---

## How to Use This Document

When making a deferred decision:

1. Find the entry here
2. Make and record the decision in the appropriate document
3. Remove the entry from this list (or mark it resolved with a reference to the resolution)

When deferring a new decision during design or implementation:

1. Add an entry here under the appropriate section
2. Cross-reference from the document where the deferral was made
3. Note what kind of resolution is needed and roughly when

The goal is that no deferred item is forgotten and no implementation work begins while critical questions are unresolved without explicit acknowledgment.
