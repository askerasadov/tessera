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

`mrz-error-taxonomy.md` lists `MrzUnknownCountryCode` as a representative warning: the issuing state or nationality code is not in the recognized lookup tables. The first validator slice did not produce this output because the SDK did not yet have a `CountryCode` value class or `CountryCodeTable`.

**Source:** First validator implementation slice; aligns with `lookup-tables.md` ("Initial Country Code Coverage").

**Resolution:** Resolved — implemented for all five formats (TD3, TD2, TD1, MRV-A, MRV-B) in `MrzValidator`. `CountryCode` value class and `CountryCodeTable` landed in `mrz-core` (per [ADR-012](decisions/0012-recognition-types-live-with-tables.md)); `CommonFields.issuingState` and `CommonFields.nationality` changed from `String` to `CountryCode`. The validator emits up to two `MrzUnknownCountryCode` warnings per document — one for `issuingState` (line 1 position 2 on every format) and one for `nationality` (position 54 for TD3 / MRV-A, 46 for TD2 / MRV-B, 45 for TD1) — distinguished by a `field: MrzField` discriminator. The categorical placement (warning, not failure) is the same as `MrzUnknownDocumentTypeCode` per [ADR-013](decisions/0013-recognition-failures-are-warnings.md). The table-completeness question is tracked separately under "Country code table completeness" below.

### Document type code recognition validation (`MrzUnknownDocumentTypeCode`)

`mrz-error-taxonomy.md` lists `MrzUnknownDocumentTypeCode`. The `DocumentType` value class and `DocumentTypeCodeTable` already exist (with a starter set), so the recognition signal is available via `DocumentType.isRecognized`. The first validator slice did not produce this output to keep the slice focused on closing the check-digit translation-owed loop.

**Source:** First validator implementation slice.

**Resolution:** Resolved — implemented for all five formats (TD3, TD2, TD1, MRV-A, MRV-B) in `MrzValidator`. The categorical placement (warning vs. validation failure) is recorded in [ADR-013](decisions/0013-recognition-failures-are-warnings.md): a recognition-table-derived check that reduces to "this code is not in our table" is a warning, because the SDK's tables are deliberately incomplete and overclaiming non-conformance would violate Principle 1 (Reader, not oracle) and Principle 4 (Honest about what we know). The check runs unconditionally for every parsed document; the warning carries the verbatim `rawCode` and the field's start position (always 0 — the document type slot is at line 1 character 1 on every format). The table-completeness question is tracked separately under "Document type code table completeness" below. The same categorical placement applies to `MrzUnknownCountryCode` (resolved above).

### Date-in-calendar validation (`MrzDateNotInCalendar`)

`mrz-error-taxonomy.md` lists `MrzDateNotInCalendar` as a representative validation failure: a date is structurally well-formed (six digits) but does not represent a real calendar date (e.g., February 30). The current parser already tolerates this — `MrzDate.parseBirth` and `parseExpiry` return `MrzDateInferenceMethod.RAW_ONLY` when the components do not form a valid date — but the validator does not surface a failure for it.

**Source:** First validator implementation slice; aligns with `mrz-validation.md` "Layer 3 — Semantic" and `mrz-error-taxonomy.md`.

**Resolution:** Resolved — implemented for all five formats (TD3, TD2, TD1, MRV-A, MRV-B) in `MrzValidator` for both `dateOfBirth` and `dateOfExpiry`. The dispatch is signal-driven from a new tri-state property on the model: `MrzDate.componentsFormCalendarDate: Boolean?`. The original `RAW_ONLY` enum value collapsed three distinct failure modes; the new property disambiguates them so the validator can emit `MrzDateNotInCalendar` only for the "components numeric but no calendar date" case, leaving "non-numeric components" (Layer-1 territory) and "calendar-valid but outside the parser's inference window" (a date that IS in the calendar) untouched. See `docs/features/mrz-data-model.md` "MrzDate" and `docs/features/mrz-validation.md` "Status of Implementation."

### Expiry-date warnings (`MrzExpiryDatePast`, `MrzExpiryDateImplausiblyFar`)

`mrz-error-taxonomy.md` lists `MrzExpiryDatePast` and `MrzExpiryDateImplausiblyFar` as representative warnings. The first validator slice produces no warnings (`ValidationResult.warnings` is always empty for now); these are the natural first warning slice.

**Source:** First validator implementation slice; aligns with `mrz-validation.md` "Date Range Conventions" and `mrz-error-taxonomy.md`.

**Resolution:** Resolved — both warnings are implemented for all five formats (TD3, TD2, TD1, MRV-A, MRV-B) in `MrzValidator`. `MrzValidator.validate(...)` accepts an explicit `referenceTime` (defaulting to `Clock.System.now()`); each format-specific parser threads its own `referenceTime` through. `MrzExpiryDateImplausiblyFar` carries `thresholdYears` (defaulting to 10) on the warning itself. Configurability of the threshold is its own deferred question — see "Validator options (configurable thresholds)" below.

### Validator options (configurable thresholds)

`mrz-validation.md` "Date Range Conventions" commits to thresholds being "configurable through the validator's options, with the documented defaults applied when no configuration is provided." The first warning slice ships the implausibly-far threshold as a private constant in `MrzValidator` (10 years, matching `mrz-error-taxonomy.md`). Building a `ValidationOptions`-style surface now would be a guess about which other thresholds eventually need configuring (Principle 11 — internal first, promote when justified).

**Source:** First warning implementation slice; aligns with `mrz-validation.md` "Date Range Conventions".

**Resolution:** When a second configurable threshold lands (likely the date-of-birth `MAX_PLAUSIBLE_AGE_YEARS` cap, or expiry-window thresholds revisited under real-world data), introduce a `ValidationOptions` value class with named, defaulted properties and a single `MrzValidator.validate(document, referenceTime, options)` overload. Keep the defaults exactly matching the current private constants so the addition is non-breaking (Principle 9).

### TD1 validator path

The first validator slice handles only TD3. For TD1 inputs, `MrzValidator.validate(...)` returns an empty `ValidationResult` (no failures, no warnings) because TD1 has no parser yet, so there is no integration test path that would exercise a TD1 validator end-to-end. Implementing TD1's composite check digit formula without a TD1 parser to drive it would produce code that compiles and runs but is not meaningfully tested against real parsed input.

**Source:** First validator implementation slice; aligns with the TD1 data-class-only state (PR #1 slice 8).

**Resolution:** Resolved — the TD1 parser and validator landed together in the TD1 parser slice. `MrzValidator.validate(...)` dispatches `is TD1` to a real `validateTD1` (replacing the previous empty-result stub) covering per-field check digits, composite check digit, sex range, calendar-date validation, expiry warnings, birth-age warning, recognition warnings, and name truncation — all driven by integration tests through `MrzParser.parseTD1`. See `docs/features/mrz-validation.md` "Status of Implementation" for the table.

---

## Deferred to a Future Release

These questions concern functionality that is intentionally not in the current scope. They are tracked here so they are not forgotten when their release approaches.

### Lenient and tolerant parsing modes

The parser currently operates in strict mode only. Lenient mode (tolerating real-world deviations such as extra whitespace) and tolerant mode (recovering from OCR confusions using check-digit-guided disambiguation) are intentionally deferred. They are additive capabilities; the strict-only API does not constrain their later addition.

**Source:** `mrz-parsing.md` ("Strictness")

**Resolution:** Partially resolved (2026-05-29 0.2.0 pre-release review, [ADR-020](decisions/0020-camera-reading-architecture.md)). **Lenient mode ships in 0.2.0** alongside live camera — consumer-chosen, with strict remaining the default and raw values always preserved; lenient forgives benign format noise without changing any value. **Tolerant mode** (check-digit-guided OCR disambiguation) is deferred to **0.3.0** (pre-captured still-image reading, where there is no next frame to retry) and, when built, must *surface* candidate corrections rather than silently overwrite (reader-not-oracle). Live camera handles OCR noise via strict-parse-and-retry across frames.

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

### Per-language conditionals in non-Latin transliteration

ICAO Doc 9303 Part 3 Section 6 (Annex G) defines transliteration tables for Cyrillic (§6.B) and Arabic (§6.C) scripts in addition to Latin (§6.A). Several entries in those tables carry per-language conditionals — recommendations that differ depending on which language the name is in. As examples in §6.B: `Г` transliterates to `G` except for some languages where it transliterates to `H`; the first character of certain names follows a different rule in Ukrainian than in other Cyrillic-using languages; certain Serbian-language conventions diverge from the general Cyrillic rules. Arabic §6.C has similar per-language structure.

The SDK's `TransliterationProfile` interface in `0.1.0` does not model a "primary language of the name being transliterated" parameter. The interface assumes one identifier per profile (typically a country code), which is sufficient for the Latin-only `0.1.0` coverage where Annex G's recommendations are uniform per codepoint. Extending to Cyrillic / Arabic raises the design question of how to surface language-conditional rules: an additional profile parameter, sub-profiles selected by language, a `LanguageHint` enum, or some other mechanism.

**Source:** Pre-`0.1.0`-tag conformance audit (2026-05-18, `CONFORMANCE-NOTES-2026-05-18.md` finding F12) — surfaced during Phase 2 review of Annex G when comparing the Latin-only `buildIcaoLatinMappings` against the full Annex G scope.

**Resolution:** Resolve when non-Latin script profiles ship (post-`0.1.0`, no scheduled release yet). The resolution must (a) name the mechanism the SDK uses to express language-conditional rules, (b) update the `TransliterationProfile` interface and `TransliterationProfileRegistry` if a new selection axis is required, (c) ensure the chosen mechanism is non-breaking to consumers who already use Latin-only profiles. Cross-reference from `transliteration.md` when the first non-Latin profile design begins.

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

### GitHub repository topics for discoverability

The repository on GitHub has no topics set, which limits discoverability through GitHub's topic search and the homepage's topic-based recommendations. Candidate topics include `kotlin`, `kotlin-multiplatform`, `mrz`, `icao-9303`, `passport`, `identity-document`; per-release additions follow as new capabilities land (e.g., `nfc` and `emrtd` when 0.6.0 ships, `android` and `ios` when platform-I/O modules activate). The question deferred is *which* topics and *when*, not *whether* to have them.

**Source:** `SESSION-HANDOFF-2026-05-21-1348-v-0-1-0-shipped-and-protected.md` "Things to Watch For" carry-forward; reaffirmed in the 2026-05-22 session close-out conversation.

**Resolution:** Pick an initial set and apply via `gh repo edit --add-topic ...`. Establish a maintenance rhythm of reviewing topics at each release milestone — add topics as capabilities land, remove ones that no longer describe scope. Either a small follow-up PR or fold into the next release-prep pass.

### Android target configuration on core modules

Core modules are scaffolded with the JVM target only. The Android target is intentionally deferred until 0.2.0 work begins, when the first Android-touching module (`mrz-camera-android`) is introduced and AGP needs to be added to the build anyway. Adding `androidTarget()` to the pure-logic core modules earlier would buy only theoretical insurance against Android-incompatible APIs sneaking into `commonMain`, at the cost of pulling AGP and its version-coupling constraints into the build before they earn their keep (Principle 2: the option that assumes less wins; Principle 11: don't promote infrastructure before it's justified). The Android SDK is already installed on the development machine; the deferral is by intent, not by tooling gap.

**Source:** Pre-implementation scaffolding session; aligns with Principles 2 and 11.

**Resolution:** Resolved (2026-05-29 0.2.0 pre-release review) — the Android target is enabled on the core modules per [ADR-017](decisions/0017-mobile-targets-and-build-stack.md), implemented in the 0.2.0 build-foundation slice. **The `androidTarget()` approach in this entry's original resolution is superseded:** on Kotlin 2.3.21 + AGP 9 the `androidTarget` block errors, so the target is added via Google's `com.android.kotlin.multiplatform.library` plugin instead.

### Platform I/O and UI module scaffolding

The pre-implementation checklist names `mrz-camera-{platform}`, `emrtd-nfc-{platform}`, and `mrz-camera-ui-{platform}` modules as scaffold targets. They are not scaffolded in 0.1.0 because each requires its corresponding platform toolchain (AGP for Android variants, Xcode for iOS variants) and there is no implementation in 0.1.0 that would exercise an empty-shell module. Empty platform modules add build configuration that has to be maintained without delivering any value until the corresponding feature work begins.

**Source:** Pre-implementation scaffolding session; aligns with `architecture.md` ("as appropriate" wording in the checklist) and Principle 11.

**Resolution:** Partially resolved (2026-05-29) — the **camera I/O modules (`mrz-camera-android`, `mrz-camera-ios`) are scaffolded in 0.2.0** with their first implementation, per [ADR-017](decisions/0017-mobile-targets-and-build-stack.md) and [ADR-020](decisions/0020-camera-reading-architecture.md). The remaining named modules stay on their roadmap schedule (NFC I/O `emrtd-nfc-{platform}` at 0.6.0; UI `mrz-camera-ui-{platform}` at 0.5.0). Keep this entry until those land.

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

**Resolution:** Resolved — fifteen ADRs exist in `docs/decisions/` as of the `0.1.0` tag. See [`docs/decisions/README.md`](decisions/README.md) for the current index. Additional ADRs may be added in the future as new significant decisions are made.

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

### External spec data licensing strategy

ICAO Doc 9303 (Machine Readable Travel Documents) is freely downloadable as PDFs from `icao.int` in six languages — see the [Doc 9303 page](https://www.icao.int/publications/doc-series/doc-9303). All 13 parts are accessible without payment or registration. The technical content the SDK needs is in:

- **Part 3** — common specs: document type codes (Section 4), country codes (Section 5), transliteration tables (Section 6 / Annex G), MRZ alphabet, check digit algorithm
- **Part 4** — TD3 (passports), including the canonical sex character set (§4.1)
- **Parts 5–7** — TD1, TD2, MRV-A, MRV-B specifics

**Reading the spec is unambiguously fine.** The SDK's algorithms (check digit, alphabet, format layouts) were implemented from the spec's technical descriptions — algorithms are not copyrightable.

**Embedding the spec's data tables verbatim is the open question.** ICAO's stated copyright position (per the site copyright notice) is restrictive: *"None of the materials provided on this web site may be used, reproduced or transmitted, in whole or in part, in any form or by any means... without permission in writing from ICAO."* Whether technical tables qualify as facts (uncopyrightable in many jurisdictions) versus creative compilations (copyrightable) is a legal question the project has not yet resolved. The conservative path for an Apache-2.0 open-source project is to avoid verbatim ICAO content until the question is settled.

This was the umbrella entry for four related downstream questions on which it bore directly:

- **Sex value canonical set per ICAO Doc 9303** — resolved by reading Part 4 §4.2.2.2 during the pre-tag audit; see the entry below.
- **Document type code table completeness** — populated from Part 4 §4.4 (the harmonized P-prefix set) and Part 5 Appendix B (the `AC` Crew Member Certificate code); see the entry below.
- **Country code table completeness** — populated from the published ISO 3166-1 alpha-3 list and ICAO Doc 9303 Part 3 §5 extensions; see the entry below.
- **Transliteration profile coverage completeness** — Latin section (Annex G §6.A) populated to full coverage; non-Latin scripts (Cyrillic, Greek, Arabic) remain deferred to a future release; see the entry below.

**Source:** Pre-`0.1.0`-tag recap (2026-05-18) — the audit established that the spec is accessible and corrected a prior framing in this document ("no authoritative copy on hand") that was incorrect.

**Resolution:** The project's operative posture from the pre-tag conformance pass is **cite-and-implement**: read Doc 9303, implement the technical content based on our understanding, cite section numbers in code KDoc and feature docs. This is path (4) above — facts-not-copyrightable, applied to the specific technical tables shipped in `0.1.0` (ISO 3166-1 alpha-3 list, the §5 extensions, the harmonized document codes, and the Annex G Latin transliteration table). The umbrella's original "stay deferred" framing was over-cautious; it served its purpose by forcing the conversation that produced the cite-and-implement posture.

What remains for `1.0.0`:

1. **Legal review of the cite-and-implement posture before public release.** The specific scope: confirm that the technical data shipped in `0.1.0` (and any further bulk additions from Doc 9303 before `1.0.0`) is defensible under the project's Apache-2.0 license terms. Path (1) — requesting ICAO permission — remains available if review surfaces a concern.
2. **Non-Latin transliteration coverage.** Cyrillic, Greek, and Arabic tables (Annex G §6.B, §6.C, §6.D) are not in `0.1.0`. When added, the same cite-and-implement posture applies. See the "Transliteration profile coverage completeness" entry below for the related design question on per-language conditionals.

Paths (2) — alternative sources — and (3) — defer to consumer-provided data — are no longer in active consideration for the spec-derived data the SDK ships, though they remain options for future tables where licensing concerns become acute.

Each downstream entry below records its own resolution against this posture.

### Specific document type implementations

Some document types are in scope but their specific format details require documentation that may not be currently public. The architecture supports them; implementation is added when documentation becomes available.

**Source:** `scope.md` ("Specific Document Implementations")

**Resolution:** Implement each as documentation becomes available.

### Sex value canonical set per ICAO Doc 9303

`mrz-error-taxonomy.md` lists the valid sex characters as `M`, `F`, `<`, or `X`. The first validator slice uses this set as the allowed characters for `MrzInvalidSexValue`. ICAO Doc 9303 Part 4 §4.1 historically lists `M`, `F`, `<`; later guidance is reported to permit `X` for non-binary documents, and some issuing states use it.

**Source:** First validator implementation slice; aligns with `mrz-error-taxonomy.md` representative-examples list.

**Resolution:** Resolved — Part 4 §4.2.2.2 (with equivalents in Parts 5/6/7) was read during the pre-`0.1.0`-tag conformance audit (2026-05-18, `CONFORMANCE-NOTES-2026-05-18.md` finding F16). The canonical MRZ sex characters per the 2021 Eighth Edition are `M`, `F`, `<` only — `X` is reserved for the VIZ per each part's Note p / Note f. Because real-world practice has adopted `X` in the MRZ for non-binary documents, the validator continues to accept `X` (Principle 1 — reader, not oracle) but now emits a new `MrzSexCharacterX` warning surfacing the spec deviation; `MrzInvalidSexValue` still fires for genuinely invalid characters. The new warning matches the existing `MrzPersonalNumberCheckDigitFiller` pattern for documented real-world deviations. Strict consumers who require literal spec conformance check `warnings.isEmpty()`. See the CHANGELOG `[0.1.0]` section.

### Document type code table completeness

The `DocumentTypeCodeTable` in `mrz-core` originally shipped with a starter set of six codes (`P`, `V`, `I`, `PP`, `PD`, `PS`) — not the complete enumeration committed to in `docs/features/lookup-tables.md` ("Initial Document Type Code Coverage"). The full Part 4 §4.4 harmonized P-prefix set and Part 5 Appendix B `AC` code were absent.

**Source:** First implementation slice for `DocumentType` (2026-05-04 session); aligns with `lookup-tables.md` coverage commitment.

**Resolution:** Resolved — populated during the pre-`0.1.0`-tag conformance audit (2026-05-18, `CONFORMANCE-NOTES-2026-05-18.md` findings F14, F15, F17). The table now contains: legacy single-character codes (`P`, `V`, `I`); the full Part 4 §4.4 harmonized P-prefix set (`PP`, `PE`, `PD`, `PO`, `PR`, `PT`, `PS`, `PL`, `PM`); and the Part 5 Appendix B `AC` Crew Member Certificate code. ~13 entries total. The `PS` displayName was corrected from "Service passport" (the original mislabeling) to "Stateless passport" per Part 4 §4.4. State-specific second-character TD1 / TD2 codes (where Parts 5/6 leave the second character to the issuing state's discretion — only the first character `A`, `C`, or `I` is fixed) are intentionally not enumerated; that open-endedness is documented in `DocumentTypeCodeTable.kt` and `lookup-tables.md`. See the CHANGELOG `[0.1.0]` section.

### Country code table completeness

The `CountryCodeTable` in `mrz-core` originally shipped with a starter set of five ISO 3166-1 alpha-3 state codes (`USA`, `GBR`, `DEU`, `FRA`, `JPN`) — not the complete enumeration committed to in `docs/features/lookup-tables.md` ("Initial Country Code Coverage").

**Source:** First implementation slice for `CountryCode` (2026-05-06 session); aligns with `lookup-tables.md` coverage commitment.

**Resolution:** Resolved — populated during the pre-`0.1.0`-tag conformance audit (2026-05-18, `CONFORMANCE-NOTES-2026-05-18.md` finding F7). The table now contains the full ISO 3166-1 alpha-3 list (~249 entries verified against the published ISO 3166/MA listing) plus the ICAO Doc 9303 Part 3 §5 extensions (Parts A through H — British nationality classes GBD/GBN/GBO/GBP/GBS, Kosovo `RKS`, European Union `EUE`, UN documents UNO/UNA/UNK, other international organizations XPO/XES/XMP/XOM/XDC, stateless and refugee codes XXA/XXB/XXC/XXX, the deprecated `ANT` and `NTZ` retained for documents still in circulation per Part F, the synthetic `UTO` specimen code per Part G, and ICAO's `IAO` code per Part H). ~272 entries total, each categorized per `CountryCodeCategory` (STATE / ORGANIZATION / STATELESS / REFUGEE / HISTORICAL / OTHER). See the CHANGELOG `[0.1.0]` section.

### Transliteration profile coverage completeness

The transliteration profiles that ship in `mrz-core` (`IcaoDefaultTransliterationProfile` and `AzeTransliterationProfile`) draw their Latin-script mappings from a shared internal helper (`buildIcaoLatinMappings()`). The Latin portion of ICAO Doc 9303 Part 3 §6.A (Annex G) is now covered in full as of the pre-`0.1.0`-tag conformance audit (2026-05-18, `CONFORMANCE-NOTES-2026-05-18.md` finding F5, with the F2 schwa correction and F13 codepoint-disambiguation cross-check). Non-Latin scripts (Cyrillic §6.B, Arabic §6.C, and the Greek table) are not yet implemented.

Per-profile overrides on top of the shared table evolved in two passes. **At 2026-05-18 (PR #43):** `AzeTransliterationProfile` overrode only the schwa pair (the load-bearing divergence ADR-009 originally called out), inheriting Annex G no-expansion for everything else. **At 2026-05-19 (pre-tag empirical pass):** AZE practice was verified against sample documents + fluent-speaker testimony + the [ALA-LC romanization table](https://www.loc.gov/catdir/cpso/romanization/azerbaij.pdf), which revealed a systematic phonetic Anglicization pattern. `AzeTransliterationProfile` now ships 8 overrides (`Ə/ə → A`, `Ç/ç → CH`, `Ğ/ğ → GH`, `Ş/ş → SH`, `X/x → KH`, `C/c → J`, `J/j → ZH`, `Q/q → G`). The four overrides on letters already in the MRZ alphabet (`C`, `J`, `Q`, `X`) required the profile to consult its override map before the MRZ-alphabet passthrough check — see ADR-009 "Implementation Note: Override Lookup Order".

Both profiles' fallback policy is to map any unmapped character to the filler `<`, so partial coverage of non-Latin scripts is safe: a consumer transliterating a Cyrillic or Arabic name through the current profiles gets filler output rather than a runtime failure. Adding entries to the underlying tables (or new per-script profiles) is a non-breaking change provided the existing mappings stay stable.

**Source:** First implementation slice for `TransliterationProfile` (2026-05-17 session); aligns with `docs/features/transliteration.md` ("The ICAO Default Profile", "Country-Specific Profiles") and ADR-009.

**Resolution:** Latin section resolved (PR-4 of the conformance pass). Non-Latin scripts remain deferred to a post-`0.1.0` release. When they are added, three resolution points come up:

1. **Per-language conditionals.** §6.B and §6.C use per-language exceptions that the `0.1.0` profile interface does not model — see the new "Per-language conditionals in non-Latin transliteration" entry under "Deferred to a Future Release" above.
2. **Profile structure.** Decide whether non-Latin tables belong inside the existing default profile (treating Cyrillic / Arabic as universal) or in separate per-script profiles selected explicitly by the consumer (parallel to country-specific overrides).
3. **AZE `Ö` / `Ü` empirical verification.** The current no-expansion inheritance is recorded as a working choice pending real-document evidence; see the entry below.

Country-specific profile expansions for additional issuing states ship per consumer demand.

### No publicly-findable regulation on MRZ transliteration for the issuing state coded AZE

Per the 2026-05-18 conformance audit's Phase 4 research, no specific regulation defining MRZ name transliteration for the issuing state coded `AZE` was located in publicly-searchable sources (searches against the state's publicly-available legal information system, cabinet-level resolutions, migration-service references, BGN/PCGN, and ECHR case-law sources). The original `AzeTransliterationProfile` (single schwa override, `Ə/ə → A`) was justified via the BGN/PCGN + ICAO chain plus observed practice.

The 2026-05-19 pre-tag pass extended the profile to 8 systematic overrides covering AZE's phonetic Anglicization pattern (see [ADR-009](decisions/0009-transliteration-profiles.md) for the full reframe). The citable basis is now stronger than at conformance time:

- **Primary source: ALA-LC romanization table for the AZE Latin alphabet** (US Library of Congress / British Library standard). ALA-LC produces `ch`, `gh`, `kh`, `sh`, `ġ` (for Q), `ă` (for Ə), `ı̐` (for I), `i` (for İ), `ȯ` (for Ö), `u̇` (for Ü). When the MRZ alphabet strips ALA-LC's diacritics to ASCII, the result matches every observed AZE encoding (for the letters ALA-LC covers).
- **Secondary source: empirical sample documents** (passport + 2 ID cards) verified the rules for `Ç`, `Ğ`, `İ`, `I`, `Ə` directly.
- **Tertiary source: fluent speaker's testimony with worked examples** verified `X → KH`, `Ş → SH`, `Q → G`, `J → ZH`, `C → J`.

A specific government regulation is still not in publicly-searchable form. Two possibilities remain equally consistent with the search outcome: (a) no specific regulation exists and the issuing authority follows ICAO + a local convention captured by ALA-LC; (b) a regulation exists but is not publicly accessible. The project's posture no longer depends on resolving this — the profile rests on ALA-LC plus the corroborating evidence.

**Source:** Pre-`0.1.0`-tag conformance audit (2026-05-18, finding F23); 2026-05-19 empirical update.

**Resolution:** Partially resolved. The substantive question (what does AZE encode in the MRZ?) is answered by the ALA-LC chain + corroborating evidence. The narrower question (is there a citable national regulation?) is open; revisit if such a regulation surfaces and update `AzeTransliterationProfile` KDoc + [ADR-009](decisions/0009-transliteration-profiles.md) accordingly. No `0.1.0` action required.

### AZE profile `Ö` / `Ü` empirical verification

ICAO Annex G recommends multiple permitted transliterations for two of the letters in the Roman alphabet of the issuing state coded `AZE`:

- `Ö ö` → `OE` or `O` (state picks)
- `Ü ü` → `UE` or `UXX` or `U` (state picks)

The other AZE-relevant Latin letters (`Ç`, `Ğ`, `İ`, `ı`, `Ş`) had unambiguous Annex G recommendations under no-expansion at conformance time. `AzeTransliterationProfile` originally inherited the `IcaoDefaultTransliterationProfile`'s no-expansion choices (`Ö → O`, `Ü → U`) parsimoniously, given the absence of evidence to the contrary.

**Source:** Pre-`0.1.0`-tag conformance audit (2026-05-18, `CONFORMANCE-NOTES-2026-05-18.md` finding F25) — Phase 4 AZE-profile law research outcome.

**Resolution:** Resolved (2026-05-19). A fluent speaker's confirmation grounded in observed practice and the [ALA-LC romanization table](https://www.loc.gov/catdir/cpso/romanization/azerbaij.pdf) (which gives `Ö → ȯ` and `Ü → u̇`, both single-character with diacritic — stripping to `O` and `U` under MRZ ASCII) converge on the no-expansion form. The AZE profile inherits `Ö → O` and `Ü → U` from `IcaoDefaultTransliterationProfile` unchanged. (The broader empirical pass that resolved this also surfaced 7 other AZE overrides — see the "Transliteration profile coverage completeness" entry above for the full set.)

### AZE profile `J → ZH` and `C → J` empirical basis

Of the 8 overrides in `AzeTransliterationProfile`, five (`Ç → CH`, `Ğ → GH`, `Ş → SH`, `X → KH`, `Q → G`) are derivable from the ALA-LC romanization table (Library of Congress standard) and corroborated by either sample documents or worked examples. Two — `J → ZH` and `C → J` — are not in ALA-LC's explicit table (ALA-LC treats both as plain Latin letters that pass through). They were added based on:

- A fluent speaker's testimony with worked examples (`Jalə → ZHALA`, `Cəlal → JALAL`)
- The phonetic Anglicization principle that explains the other 6 overrides (AZE J is /ʒ/ = English "zh"; AZE C is /dʒ/ = English "j")
- Internal consistency: in the AZE profile, every letter whose source phonetic value diverges from the corresponding English letter is overridden, so leaving J and C alone would be inconsistent with the systematic pattern

The two overrides remain the empirically weakest links in the profile. If a future sample passport contains either letter in the name field and shows passthrough (`J → J`, `C → C`) instead, the overrides should be removed.

**Source:** 2026-05-19 pre-tag empirical pass.

**Resolution:** Confirm against a real sample document containing `J` or `C` in the name field when available. Either confirm current behavior (no change) or remove the override and update `AzeTransliterationProfile` KDoc + [ADR-009](decisions/0009-transliteration-profiles.md). Tracking so the gap is not forgotten.

### Driver's license format choice (mDoc vs proprietary)

When driver's license NFC reading is added in a future release, the choice between standard mDoc-compliant licenses (ISO 18013-5) and proprietary national formats depends on which markets the project prioritizes.

**Source:** `scope.md` ("Beyond 1.0")

**Resolution:** Decide when driver's license NFC work begins, based on consumer needs at that time.

### Trust anchor source for chip signature verification

Cryptographic verification of NFC chip signatures requires trust anchors (typically Country Signing Certificate Authority certificates, distributed via the ICAO Public Key Directory or similar). The choice of trust anchor source is its own design problem, deferred until chip signature verification is on the active roadmap.

**Source:** `scope.md` ("Beyond 1.0")

**Resolution:** Design when chip signature verification is added.

### Distribution channels (Maven Central, CocoaPods, SPM)

**JVM coordinate shape, lockstep versioning, BOM, first-publish version, and first-publish scope are resolved by [ADR-016](decisions/0016-maven-coordinates-and-first-publish.md).** The published groupId is `io.lightine.tessera` (backed by the verified Sonatype namespace at `io.lightine`); artifactIds follow the `tessera-<module>` convention; modules version in lockstep with a `tessera-bom` artifact for version alignment; the first Maven Central publication shipped at 0.1.1 (published 2026-05-29) with all five current modules plus the BOM; no snapshot builds at 0.x.

What remained open under this entry — the iOS distribution channel (CocoaPods vs Swift Package Manager) — is now resolved (see Resolution). The only distribution question still future is the **web (JS/Wasm) channel (npm)**, decided when/if the web target activates.

**Source:** Implicit; not yet referenced.

**Resolution:** JVM distribution resolved by [ADR-016](decisions/0016-maven-coordinates-and-first-publish.md) and **executed** — `io.lightine.tessera:*:0.1.1` was published to Maven Central on 2026-05-29 (publishing slices in PRs [#88](https://github.com/lightine-io/tessera/pull/88)–[#90](https://github.com/lightine-io/tessera/pull/90)). iOS distribution is resolved (2026-05-29) — **Swift Package Manager** (Kotlin/Native XCFramework wrapped as a Swift package; CocoaPods rejected as legacy) per [ADR-019](decisions/0019-ios-distribution-via-spm.md), within a **one-channel-per-ecosystem** model (Maven Central for JVM/Android/desktop, SPM for iOS, npm for web when that target activates); execution lands in the 0.2.0 iOS slice.

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

Core modules (`mrz-core`, `emrtd-core`, `types`, `telemetry`, `logging`) are scaffolded with the JVM target only. Configuring the iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) requires Xcode, which is not installed on the development machine where scaffolding was performed. There is no design decision to make — the targets are committed in `architecture.md` and ADR-002. The deferral is purely about toolchain availability.

**Source:** Pre-implementation scaffolding session; depends on Xcode install.

**Resolution:** Resolved (2026-05-29 0.2.0 pre-release review) — **Xcode is now present** (26.5 on the development machine), lifting the toolchain gate noted above ("not installed" is no longer true). The three iOS targets are enabled on the core modules per [ADR-017](decisions/0017-mobile-targets-and-build-stack.md), with the Normalization `expect`/`actual` ([ADR-014](decisions/0014-unicode-normalization-strategy.md)) gaining an iOS `actual`; the committed iOS deployment minimum is **18** ([ADR-018](decisions/0018-platform-minimums-and-managed-raise.md)), not the 15.0 this entry's original text referenced. Implementation lands in the 0.2.0 iOS build-foundation slice.

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

Add a LESSONS.md or RETROSPECTIVE.md capturing what went well and what didn't, after each release or milestone. Useful for long-term project health.

**Trigger:** After the first internal release (0.1.0) ships, when there is actually a milestone to retrospect on.

### Code precedent examples

Once implementation has produced idiomatic code in the project, consider whether to extract small example snippets into the documentation as "this is what a parser implementation in this project looks like." Not pre-written — emerges from real first implementations.

**Trigger:** After 0.1.0 lands, if Claude Code consistently produces non-idiomatic code that requires correction.

### Runnable camera sample app

A small runnable sample app (Android first, iOS later) that integrates the headless camera reader end-to-end — points the camera at a synthetic MRZ and prints the parsed result. It would double as the on-device test harness for the 0.2.0 camera work *and* as living integration documentation (ties to "Code precedent examples" above). 0.2.0 ships written integration docs (snippets + a standalone guide) instead; a runnable sample is deferred to keep 0.2.0 scoped to the headless SDK plus docs.

**Source:** 2026-05-29 0.2.0 pre-release review ([ADR-020](decisions/0020-camera-reading-architecture.md)); deferred from PR-F (consumer integration docs).

**Trigger:** When the headless camera reader is stable on a platform and a runnable demo would add more than the written snippets + integration guide already provide — likely late in 0.2.0 or alongside the 0.5.0 UI.

### CONTRIBUTING.md at project root

GitHub recognizes a top-level `CONTRIBUTING.md` and surfaces it on PR creation. The current `docs/conventions.md` covers what a CONTRIBUTING.md would cover. A small top-level file pointing to conventions.md may be useful when the project goes public on GitHub.

**Trigger:** Before the first public push to GitHub or equivalent.

**Resolution:** Resolved (2026-05-20). Added a short [`CONTRIBUTING.md`](../CONTRIBUTING.md) at the project root pointing to `docs/conventions.md`, `.claude/git-workflow.md`, `docs/versioning.md`, `docs/testing.md`, `docs/principles.md`, `docs/open-questions.md`, the PR template, and `SECURITY.md`. The file is intentionally short — it does not duplicate the full conventions, just makes them discoverable from GitHub's contributor flow. Landed alongside `SECURITY.md`, `.github/CODEOWNERS`, `.github/dependabot.yml`, and `.github/workflows/check.yml` in the pre-public-readiness pass.

### CHANGELOG.md initial entry

The project commits to Keep a Changelog format (see `docs/versioning.md`). The actual `CHANGELOG.md` file does not yet exist. It will be created with the first internal release entry.

**Trigger:** Before tagging 0.1.0.

**Resolution:** Resolved — `CHANGELOG.md` exists at project root in Keep a Changelog format. The initial `[0.1.0]` entry is populated by the tag commit; the `[Unreleased]` section above it accumulates entries for the next release per the conventions in `docs/versioning.md`.

### Tessera-specific security-reviewer subagent

The Claude Code optimization audit in PR [#66](https://github.com/lightine-io/tessera/pull/66) identified a security-reviewer subagent as a candidate AI-tooling addition. Deferred because at `0.1.0` (pure parsing/validation/generation) the security surface is narrow — limited to PII handling in logs/error messages, input validation on MRZ parsers, dependency hygiene, and avoiding hardcoded real-looking document data in tests. The built-in `security-review` skill that ships with Claude Code is generic-but-sufficient for this surface. Designing a Tessera-specific subagent now would produce a thin prompt-only artifact without enough code to ground its guidance against. The real security surface arrives in `0.2.0` (camera + image processing), `0.5.0` (BAC), `0.6.0` (PACE/NFC crypto), at which point a domain-aware subagent has actual patterns to enforce.

**Trigger:** During the Pre-Release Tech-Stack Review for `0.2.0` (per the [`pre-release-tech-stack-review`](../.claude/skills/pre-release-tech-stack-review/SKILL.md) skill). Decide: ship a domain-aware subagent in `.claude/agents/security-reviewer.md`, OR confirm the built-in skill remains sufficient. The decision becomes load-bearing once sensitive code starts landing.

**Resolution:** Resolved (2026-05-29 0.2.0 pre-release review) — **ship it.** A Tessera-specific `.claude/agents/security-reviewer.md` is added (read-only; *advise-don't-dictate*) with a broad mandate: PII in logs/errors, input validation, camera-buffer memory hygiene, supply-chain (dependency vulnerabilities, licenses, plugin provenance), publishing (signing, POM, no committed secrets), and repo/GitHub settings. It is paired with mechanical CI checks (dependency CVEs, secret scanning) so coverage does not depend on a session remembering to invoke it. The 0.2.0 camera/image surface is where a domain-aware reviewer earns its keep.

### Dokka multi-module aggregation and hosted docs site

The Dokka 2 wiring shipped with publishing infrastructure slice 2 generates per-module HTML javadoc jars (`tessera-types-<v>-javadoc.jar`, `tessera-mrz-core-<v>-javadoc.jar`, etc.) — one self-contained docs set per artifact, matching how Maven Central distributes attached files. The trade-off: KDoc cross-references that span modules (e.g., a `[MrzParser]` reference in `types/vocabulary/ReadMethod.kt` pointing at a class in `mrz-core`) cannot be resolved by Dokka when documenting `types` in isolation, so they render as plain text instead of clickable links in the published HTML. ~5-7 such references exist at 0.1.1; Dokka emits non-fatal warnings during `publishToMavenLocal` for each. IDE navigation (IntelliJ project model) is unaffected — only the published HTML loses click-to-navigate for these references.

The proper fix is **Dokka multi-module aggregation** + a **hosted docs site**: configure a root-level `dokka { }` block that treats all modules as one project, generate a unified HTML site with full cross-linking, and host it (GitHub Pages, Netlify, or a project-owned subdomain like `docs.lightine.io`). Per-module javadoc jars then become either minimal (own pages only) or `JavadocJar.Empty()` since the real docs live at the hosted URL referenced from each module's POM `url`. This is what major Kotlin ecosystem libraries do (kotlinx.coroutines, kotlinx.serialization at `kotlinlang.org/api/...`).

Deferred because at the project's current scale (single maintainer, narrow 0.1.x public API surface) the few unlinked cross-references in published HTML are a mild UX paper-cut, not a broken experience — and the proper fix requires standing up docs-hosting infrastructure that is its own multi-decision conversation (where the site lives, how versioning works in URLs, what CI publishes it, what versions stay alive at the hosted endpoint). That conversation deserves its own slice rather than getting wedged into a publishing-infrastructure PR.

**Trigger:** When public-API browsing UX matters enough to justify the infrastructure work — typically around the `1.0.0` polish pass (public stability commitment lands; the API is wide enough to benefit from rich cross-module navigation; the project is mature enough to deserve a real docs site). Could also trigger earlier if external integrators provide feedback that the per-module HTML is hard to navigate. Decision form: an ADR locking the docs-hosting target + a publishing-infrastructure slice wiring up aggregation + CI deployment.

### Cross-project planning tool (YouTrack vs GitHub Projects vs current setup)

When future projects under `io.lightine` start (potentially with shared contributors), unified visibility across projects may justify a dedicated project-management tool. The current setup (GitHub Issues + `docs/open-questions.md`, `.handoffs/`, ADRs, `CHANGELOG.md`) is sufficient for a single active project; adding tooling now would create stale data and split the source of truth across more places (Principle 11 — internal/simple first, promote when justified).

Three realistic options when this is revisited:

- **Stay with current setup** — GitHub Issues per repo + the existing markdown infrastructure. Lowest overhead; works fine if cross-project coordination stays informal.
- **GitHub Projects** — free, integrated, supports cross-repo boards under a GitHub organization. Provides backlog and roadmap views without adopting a new tool or new login.
- **YouTrack** (JetBrains) — free for up to 10 users on cloud. Strongest for customizable workflows and serious project management. Highest overhead.

**Trigger:** When **both** conditions are true: (a) a second active project exists under `io.lightine` (actual code, actual work — not just an idea), and (b) cross-project visibility or coordination cost becomes a real felt pain. Until both hold, the existing infrastructure is sufficient.

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
