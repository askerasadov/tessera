# ADR-019: iOS distribution via Swift Package Manager

**Status:** Accepted (execution deferred to the iOS slice)

---

## Context

`0.2.0` ships Tessera for iOS ([ADR-017](0017-mobile-targets-and-build-stack.md)). The JVM and Android artifacts are distributed through Maven Central ([ADR-016](0016-maven-coordinates-and-first-publish.md)), but **native Swift/iOS consumers do not use Maven Central or Gradle** — Xcode integrates dependencies through CocoaPods or Swift Package Manager (SPM). To let an iOS developer consume Tessera, it must be published in a form their toolchain understands. The open question "Distribution channels" ([`open-questions.md`](../open-questions.md)) left the iOS channel undecided.

## Decision

**Distribute the iOS build via Swift Package Manager.** Kotlin/Native produces an **XCFramework**, wrapped in a **Swift package** (`Package.swift` referencing the framework) that an iOS developer adds in Xcode. CocoaPods is **not** used.

This sits within a **one-channel-per-ecosystem** distribution model, all fed from the single KMP codebase and **lockstep-versioned** (extending [ADR-016](0016-maven-coordinates-and-first-publish.md)):

| Consumer ecosystem | Channel |
|---|---|
| JVM, Android, desktop-JVM | Maven Central (already live) |
| iOS / Swift | **SPM** (XCFramework → Swift package) |
| Web (JS/Wasm) | npm (future, when the web target activates) |

**Execution is deferred to the iOS implementation slice.** One slice-time detail — whether `Package.swift` lives in this repository or a dedicated distribution repository — is left open here; both are viable and the choice does not affect this decision.

## Consequences

**Positive:**
- iOS consumers integrate Tessera through their native, first-party dependency manager.
- One codebase, multiple ecosystem channels, all on the same version — no divergence.
- Avoids CocoaPods, which is in reduced maintenance.

**Negative:**
- SPM distribution of a KMP binary (XCFramework + Swift package, often via a Git repo rather than a central registry) is more bespoke than Maven Central's central-repository model; the packaging mechanics are built at the iOS slice.
- The Swift-facing API surface depends on Kotlin/Native's Objective-C/Swift export; a clean Swift experience requires the headless API to be designed Swift-friendly (see [ADR-020](0020-camera-reading-architecture.md)).

**Neutral:**
- The JVM / Android distribution (Maven Central) is unchanged.

## Alternatives Considered

- **CocoaPods.** Rejected: effectively legacy / reduced-maintenance; SPM is Apple's official, Xcode-integrated manager and the forward-looking choice.
- **Both SPM and CocoaPods.** Rejected for now: doubles packaging maintenance for a declining channel; revisit only if real consumer demand for CocoaPods appears.

## Related Decisions

- [ADR-016](0016-maven-coordinates-and-first-publish.md) — Maven coordinates and lockstep versioning; this extends the distribution model to iOS.
- [ADR-017](0017-mobile-targets-and-build-stack.md) — enables the iOS target whose output this distributes.
- [ADR-020](0020-camera-reading-architecture.md) — the Swift-friendly headless API that determines the SPM consumer's experience.

## Related Documents

- [`../scope.md`](../scope.md) — distribution channels.
- [`../architecture.md`](../architecture.md) — iOS modules and outputs.
- [`../open-questions.md`](../open-questions.md) — "Distribution channels" (iOS portion resolved here; npm/web remains future).
