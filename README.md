# Tessera

A vendor-neutral SDK for reading, validating, and generating identity document data.

Tessera reads Machine Readable Zones (MRZ) from passports, national ID cards, residence permits, machine-readable visas, and similar travel documents conforming to ICAO Doc 9303. It returns extracted data verbatim, with structured validation results — leaving all trust decisions to the integrating application.

> **Status:** In development. Not yet released. The first public release is targeted for version 1.0.0; pre-release versions in the 0.x line are available to early integrators with the same backward-compatibility commitments as post-1.0 versions.

---

## What it does

- **Reads MRZ** from live camera, pre-captured images, manual entry, or NFC chip (NFC support arrives in a later release)
- **Parses all ICAO Doc 9303 MRZ formats**: TD1, TD2, TD3, MRV-A, MRV-B
- **Validates** structurally, by check digit, and semantically — without making trust decisions
- **Generates** valid MRZs from structured input, supporting round-trip use cases
- **Exposes everything it extracts** — raw fields, computed values, validation results, and warnings — so the consumer always knows what was observed and what was inferred
- **Runs anywhere the core technology stack supports** — initial mobile targets (Android, iOS) plus future support for backend, desktop, and web

---

## What it deliberately does not do

Tessera is a reader, not an oracle. It surfaces observations; the consumer makes trust decisions. Specifically, the SDK does not:

- Decide whether a document is "valid" or "trustworthy" — that depends on the consumer's threat model
- Verify document authenticity against external registries
- Perform face matching or liveness detection (these may be added later as separate capabilities)
- Store any data — no persistence, no caching, no telemetry by default
- Phone home — no network calls, no analytics, no licensing checks

These are deliberate boundaries. See [`docs/principles.md`](docs/principles.md) for the reasoning.

---

## Quick example

The following is illustrative — the actual API may differ in detail. See [`docs/features/`](docs/features/) for the current contracts.

```kotlin
val result = MrzParser.parse("""
    P<UTOERIKSSON<<ANNA<MARIA<<<<<<<<<<<<<<<<<<<
    L898902C36UTO7408122F1204159ZE184226B<<<<<10
""".trimIndent())

when (result) {
    is ParseResult.Success -> {
        val doc = result.document as MrzDocument.TD3
        println("Name: ${doc.commonFields.primaryIdentifier}, ${doc.commonFields.secondaryIdentifier}")
        println("Document number: ${doc.commonFields.documentNumber}")
        // ... use the parsed data
    }
    is ParseResult.PartialSuccess -> {
        // Data extracted, but some validations failed.
        // Read result.document and result.metadata.validationFailures to decide.
    }
    is ParseResult.Failure -> {
        // The input was structurally too broken to construct a document.
        println("Parse failed: ${result.error}")
    }
}
```

The result type makes the three possible outcomes explicit. The consumer cannot accidentally treat a `PartialSuccess` as a `Success`.

---

## Documentation

The project's documentation is structured for two audiences: integrators (who want to use the SDK) and contributors (who want to understand or extend it).

### For integrators

- [`docs/scope.md`](docs/scope.md) — what the SDK supports, what it does not, and what is planned
- [`docs/features/`](docs/features/) — feature-by-feature documentation of every capability
- [`docs/reading-risks.md`](docs/reading-risks.md) — what each reading method establishes, what it does not, and what additional verification might be needed
- [`docs/glossary.md`](docs/glossary.md) — definitions of MRZ, eMRTD, BAC, PACE, and other terms used throughout the documentation
- [`docs/versioning.md`](docs/versioning.md) — versioning policy and release commitments

### For contributors

- [`docs/principles.md`](docs/principles.md) — the foundational principles every design decision honors
- [`docs/architecture.md`](docs/architecture.md) — module structure, dependency graph, and technology choices
- [`docs/conventions.md`](docs/conventions.md) — how documentation is written, how decisions are made, how contributions happen
- [`docs/decisions/`](docs/decisions/) — Architecture Decision Records capturing the reasoning behind major choices
- [`docs/open-questions.md`](docs/open-questions.md) — decisions that have been deliberately deferred, tracked so they are not forgotten

---

## Platforms

Currently targeted platforms:

- **Android** — minimum API level 26 (Android 8.0)
- **iOS** — minimum iOS 15.0

The architecture supports additional targets without changes to the core logic:

- **JVM backend** — for server-side parsing, validation, and generation
- **Web** (JS / Wasm) — for browser-side validation and generation
- **Desktop** (JVM and native) — for desktop applications

These additional targets are not part of the initial releases but can be activated when there is a use case.

---

## Versioning

Tessera follows [Semantic Versioning 2.0.0](https://semver.org/) with strict backward-compatibility commitments from the first release onward — including the 0.x line. This is stricter than the convention in many open source projects, where 0.x signals "API may change without notice." The choice is deliberate: see [`docs/versioning.md`](docs/versioning.md) for the reasoning.

---

## License

Tessera is released under the Apache License 2.0. The full license text is in the [`LICENSE`](LICENSE) file at the project root. See [`docs/decisions/0010-apache-2-license.md`](docs/decisions/0010-apache-2-license.md) for the reasoning behind the license choice.

---

## Contributing

Contribution conventions are documented in [`docs/conventions.md`](docs/conventions.md). The short version:

- Decisions of architectural or scope significance are recorded as ADRs
- Disagreement is welcome — the project's culture is dispute-driven, grounded in the principles
- New conventions are added through normal contribution: proposal, discussion, agreement, then an edit

The project is currently in pre-release development. Public contribution channels open at the 1.0.0 release.

---

## Acknowledgments

Tessera builds on the work of the International Civil Aviation Organization (ICAO), whose Doc 9303 series defines the standards this SDK implements. The SDK references those standards rather than reproducing them.

The project's design owes a debt to the broader open source community's work on identity document standards, MRZ parsing libraries that came before, and the Kotlin Multiplatform ecosystem that makes shared cross-platform logic practical.
