# Conventions

This document captures the operating rules for working on the project: how documents are written, how things are named, how decisions are recorded, how contributions happen. Where principles describe *what we value* and architecture describes *how the code is organized*, conventions describe *how we work together*.

These are conventions, not principles. They can change with reasonable cause; they are not foundational commitments. When a convention starts feeling forced or counterproductive, it is up for discussion.

This document is living. New conventions may be added as the project encounters new situations; existing conventions may be revised or removed when they no longer serve the project.

---

## Documentation Conventions

### What Every Document Must Include

Every document in `docs/` opens with a short paragraph stating what it is, who it is for, and how it relates to the broader project. A reader landing on the document with no context should be able to tell within a few sentences whether they are in the right place.

Every document explicitly declares whether it is *living* (subject to ongoing revision) or *fixed* (a snapshot of a point in time, such as an ADR after it is accepted). Living documents may evolve freely; fixed documents change only through replacement, not edit-in-place.

### What Every Feature Document Must Include

Feature documentation describes a specific capability of the SDK. Each feature document includes:

- **Purpose** — what the feature does, in one or two sentences
- **Platform availability** — which targets the feature is available on, and from which release. If the feature is target-specific (some are, by their nature), this is stated explicitly. If the feature is target-agnostic, this is stated explicitly.
- **API surface** — the public types, methods, and contracts the feature exposes
- **Inputs and outputs** — what the feature consumes and what it produces
- **Errors and warnings** — every error and warning the feature can produce, typed and named
- **Related principles** — which project principles inform this feature's design
- **Related decisions** — which ADRs are relevant to this feature

This list is the minimum. Features may include more sections (examples, diagrams, edge cases) as appropriate.

### What Every Decision Record Must Include

Architecture Decision Records (ADRs) capture significant decisions in a stable, reviewable format. Each ADR includes:

- **Title** — short, descriptive
- **Status** — proposed, accepted, deprecated, or superseded (with reference to superseding ADR if applicable)
- **Context** — what situation led to needing the decision
- **Decision** — what was decided, stated unambiguously
- **Consequences** — what becomes true (positive and negative) as a result of this decision
- **Alternatives considered** — other options weighed and why they were not chosen

ADRs are *fixed* once accepted. If a decision changes, a new ADR supersedes the old one; the old one is marked deprecated but remains in the record. The intent is that the reasoning behind every significant choice is preserved, not lost.

### Cross-References Between Documents

Documents reference each other by relative file path within the repository. References are explicit: "see `architecture.md`" rather than vague allusions like "see the architecture doc somewhere."

Principles are referenced by number: "Principle 1" or "Principle 1 — Reader, Not Oracle." Numbers are stable; the eleven principles are fixed in their numbering even if their order in the document changes.

When a document references another that does not yet exist, the reference is included anyway and the missing document is tracked as a known gap. Forward references are acceptable and signal intent; they should be resolved by writing the missing document, not by deleting the reference.

---

## Target-Agnostic Discipline

A persistent risk in this project is unconsciously assuming a specific platform. The first concrete features are mobile, but the project's commitments must hold across mobile, backend, web, desktop, and any future target.

When proposing or documenting any rule, behavior, or commitment, the question to ask is:

> *"Does this assume a specific platform, or does it apply wherever the SDK runs?"*

If a rule assumes a specific platform, one of the following must be true:

- The rule is restated as a target-agnostic principle, with platform-specific implementations documented separately
- The rule is explicitly scoped to that platform ("this applies only to the Android UI module")
- The rule is moved to platform-specific documentation, not project-wide documentation

Platform-specific examples and illustrations are welcome — but they must be examples of an underlying principle, not the principle itself.

This discipline applies to documents, code comments, public APIs, and conversations during design.

---

## Naming Conventions

### Modules

Module names follow the pattern `{domain}-{role}[-{platform}]`:

- `{domain}` identifies the subject area (`mrz`, `emrtd`, `domain`, `telemetry`, `logging`)
- `{role}` identifies what the module does (`core` for pure logic, `nfc` or `camera` for I/O, `ui` for user interface)
- `{platform}` is appended only for platform-specific modules (`android`, `ios`, etc.)

This pattern is consistent across the project. New modules added later follow the same convention.

### Packages

Within a module, internal packages reflect functional areas — for example, `parsing`, `generation`, `validation`, `transliteration`. Each internal package has a clean public API surface within its module, which makes future promotion to a standalone module mechanical (Principle 11).

Package paths use a stable root namespace owned by the project, followed by descriptive segments. The exact root namespace is finalized when the project's identity is locked for publication.

### Errors

Error types are named for the specific failure they represent. Generic catch-all names are not used.

Examples of well-named errors:

- `MrzCheckDigitFailed`
- `NfcAuthenticationFailed`
- `CameraPermissionDenied`
- `DocumentTypeUnsupported`

Examples of names that would be rejected:

- `SdkException`
- `GeneralError`
- `UnknownFailure`

Each error carries enough context to be actionable — which field, which step, which input position.

### Public vs Internal APIs

Each module declares which symbols are part of its public API. Symbols not exposed publicly are implementation detail and may change between versions without notice. The project uses Kotlin's `internal` visibility modifier to enforce this where the language supports it, and documentation conventions for cases where it does not.

Public APIs are designed to be stable across the lifetime of a major version (Principle 9). Adding to a public API is non-breaking; removing requires a deprecation cycle.

---

## Internal Packages First

The project follows Principle 11: new features start as internal packages within existing modules. They are promoted to standalone modules only when at least one of the following clearly applies:

- Independent reuse — the feature is genuinely useful without its parent module
- Independent evolution — the feature changes at a different pace than its parent
- Independent testing — the feature requires its own testing context
- Independent ownership — the feature is owned or maintained by different people
- Independent shipping — the feature releases on a different schedule
- Optional inclusion — consumers should be able to exclude the feature

Until one of these applies, a clear internal package boundary with a defined public API is enough. The promotion to a standalone module, when it happens, is mechanical because the boundary already exists.

This avoids both extremes: the monolith that grows without internal structure, and the over-modularized project where every concept is a separate artifact regardless of need.

---

## Adding a New MRZ Format

Adding a new MRZ format (beyond the ICAO Doc 9303 formats already supported) is a multi-phase exercise. Each phase is its own PR; the format moves from "data class only" through "readable" through "generatable" through "auto-detected" in deliberate increments. The phasing keeps reviews focused on one concern at a time and lets consumers benefit from partial coverage earlier than under a single monolithic PR.

A format is "added" when Phase 1 lands, "round-trip complete" when Phase 2 lands, and "complete" when Phase 3 lands. A release tag does not require every supported format to be complete in every phase, but the release notes describe what is shipped and what is deferred per format. The Status tables in the feature documents are the per-feature source of truth for what's implemented versus documented.

### Phase 1 — Reading and recognition

The format is parseable and validatable. The minimum needed for a consumer to extract typed data and see typed validation findings.

1. Define the format specification in the shared `formats/` package within `mrz-core` — field positions, field widths, check digit positions, character set rules. Implement the appropriate spec interface (`MrzFormatSpec` for visa-shape formats with no composite digit; `MrzFormatSpecWithComposite` for TD-family formats per ICAO Doc 9303 Parts 4–6)
2. Add a new variant to the `MrzDocument` sealed hierarchy reflecting the format's fields
3. Implement the format-specific parser (`MrzParser.parse{FORMAT}` overloads with the `referenceTime: Instant` parameter)
4. Implement the format-specific validator path (`MrzValidator.validate(document)` dispatch)
5. Add lock tests for the format spec, parser tests for happy paths and the documented error paths, validator tests for per-field findings, and update the `MrzDocumentTest` sealed-exhaustiveness check
6. Update the relevant feature documents — illustrative shapes and Status tables in `mrz-data-model.md`, `mrz-parsing.md`, `mrz-validation.md`
7. Update the changelog under `[Unreleased]`

The format is "added" when Phase 1 lands. Check digits in test fixtures are computed via the SDK's check-digit primitive directly (a generator is not required at this phase).

### Phase 2 — Generation

The format is generatable from a `MrzDocument` instance. Round-trip tests confirm that parse-then-generate and generate-then-parse return the original input for valid cases.

1. Implement the format-specific generator (`MrzGenerator.generate{FORMAT}` overloads)
2. Add round-trip property tests against the format's synthetic fixtures
3. Add or extend synthetic test fixtures generated through the format's own generator path, including the canonical example from the format's source specification
4. Update `mrz-generation.md` — illustrative shape and Status table — and any other affected feature docs
5. Update the changelog

The format is "round-trip complete" when Phase 2 lands.

### Phase 3 — Auto-detect integration

The format is reachable through `MrzParser.parse(input)` auto-detect. This phase exists separately because auto-detect's dispatch rules are designed with every supported format in mind, not added one at a time; a Phase 3 update typically lands as a single PR covering auto-detect integration for whichever formats are then in Phase 1 or beyond.

1. Update the auto-detect dispatch logic with structural cues that distinguish the new format from existing formats
2. Add tests covering the new dispatch path (happy path plus relevant edge cases — especially disambiguation between formats with overlapping shapes)
3. Update `mrz-parsing.md` "Auto-Detect Behavior" to describe the new cue
4. Update the changelog

The format is "complete" when Phase 3 lands.

### Cross-cutting

- `scope.md` lists the format as supported as soon as Phase 1 lands. The scope claim is that the SDK supports the format; the Status tables in feature docs record which phases are implemented.
- The same phasing applies in spirit to non-MRZ formats added in the future (chip data formats, document image formats, etc.), with the specific steps adjusted for the relevant subsystems.

---

## Code Style

Code style follows the idiomatic conventions of each target language and platform — Kotlin code reads as idiomatic Kotlin, Swift wrappers read as idiomatic Swift, and so on. The overarching commitment is: code is written to be readable by people who do not have the original author's context.

### Kotlin

The project uses the **Kotlin official code style** (`kotlin.code.style=official` in `gradle.properties`). Formatting and linting are enforced through:

- **[Spotless](https://github.com/diffplug/spotless)** — applied at the project root, formats and checks every Kotlin source file (`*.kt`) and Gradle build script (`*.gradle.kts`) across all modules.
- **[ktlint](https://github.com/pinterest/ktlint)** — Spotless's chosen backend for Kotlin. Version pinned in `gradle/libs.versions.toml`.
- **`.editorconfig`** at project root — editor-level consistency for indentation (4 spaces), line endings (LF), encoding (UTF-8), and ktlint rule overrides.

Two commands cover daily use:

- `./gradlew spotlessApply` — auto-formats every Kotlin and Kotlin Gradle file in the project.
- `./gradlew spotlessCheck` — verifies formatting; fails the build on any violation.

`spotlessCheck` runs as part of the standard `./gradlew check` and `./gradlew build` lifecycle, so style violations break the build by default.

### Dependency Upgrade Cadence

The project bumps the toolchain and dependencies to current stable on a **six-monthly cadence**: next checkpoint **2026-10-01**, then every six months after that. The exact day doesn't matter (±2 weeks is fine); the cadence is the operational rhythm, not a hard deadline.

Each cycle bumps the items pinned in `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts`, and the `jvmToolchain(N)` calls across module `build.gradle.kts` files: Kotlin (and KMP plugin), Gradle, JDK toolchain floor (when LTS situation warrants), dev tooling (Spotless, ktlint), runtime dependencies (kotlinx-datetime), test dependencies (kotest), and Gradle settings plugins (foojay-resolver-convention).

The full operational detail — what to bump, how to verify compatibility, how to split into PRs to keep blast radius small — lives in the "Dependency Upgrade Cadence" rule in [`CLAUDE.md`](../CLAUDE.md).

### Swift, other languages

Conventions for Swift wrappers and any future languages are added to this document when those source sets are introduced. The same principle applies: idiomatic per-language style, enforced by the language's standard tooling (e.g., SwiftLint for Swift), with configuration committed at the project root.

---

## Contribution Conventions

### How Decisions Are Made

Decisions of architectural or scope significance are recorded as ADRs (see "Documentation Conventions" above). Decisions about a specific feature live in that feature's documentation. Smaller decisions about implementation detail do not need a record beyond the code itself.

When a decision is contested, the path is:

1. Discussion grounded in the principles
2. Examination of the specific consequences of each option
3. Either: agreement, or escalation to maintainers if no agreement is reached

Decisions are not made by authority; they are made by reasoning that the participants can stand behind. When a maintainer decides unilaterally, the reasoning is recorded so others can engage with it.

### How Disagreements Are Resolved

Disagreement is welcome. Principle 4 (Honest about what we know) implies that confident statements should be testable; if someone disagrees, the test is whether the disagreement points at a real flaw or a difference in values.

When a contributor and a maintainer disagree:

- The disagreement is articulated specifically — what is the option being rejected, what is the option being preferred, and why
- The relevant principles are consulted
- If both options are consistent with the principles, the maintainer's preference resolves the disagreement, but the alternative is recorded for future reconsideration

This convention is light and informal; it does not need a process tool or a workflow. It is captured here so the project's culture is explicit rather than implicit.

### How New Conventions Are Added

New conventions are added to this document through normal contribution: a proposal, discussion, agreement, then an edit to this document. Conventions that are imposed without discussion tend to be ignored; conventions that are discussed and agreed upon tend to be followed.

When this document grows large enough that finding a specific convention becomes difficult, sections are split into focused sub-documents and this document becomes an index. That moment has not arrived; this convention is recorded so it is recognized when it does.

---

## How This Document Relates to Principles

Conventions implement principles (defined in `principles.md`), but they are not principles themselves. Principles are the bedrock; conventions are how we navigate day-to-day work in light of the bedrock.

When a convention seems to conflict with a principle, the principle wins and the convention is revised. When a convention seems to conflict with a different convention, that is a sign one or both conventions need refinement.

The principles do not need conventions to remain valid. Conventions need principles to be coherent.
