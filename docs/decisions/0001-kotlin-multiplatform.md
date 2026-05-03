# ADR-001: Use Kotlin Multiplatform for shared logic

**Status:** Accepted

---

## Context

The SDK provides identity document reading and validation across multiple target platforms. The first concrete targets are Android and iOS, with backend (JVM), web (JS/Wasm), and desktop targets supported by the architecture but not part of the initial releases.

The core logic — MRZ parsing, generation, validation, lookup tables, transliteration, chip data parsing, protocol implementations — is genuinely platform-independent. The same parser produces the same output regardless of where it runs. The same check digit algorithm computes the same result on a phone, a server, or a web browser.

Three approaches were considered for sharing this logic across platforms:

1. Duplicate the implementation per platform (e.g., Kotlin for Android, Swift for iOS, TypeScript for web)
2. Use a non-Kotlin cross-platform framework (Rust with bindings, C++ with bindings, etc.)
3. Use Kotlin Multiplatform (KMP) to compile a single Kotlin codebase to multiple targets

A decision was needed before any meaningful implementation work began.

---

## Decision

The SDK uses Kotlin Multiplatform for all shared core logic. Core logic modules (`mrz-core`, `emrtd-core`, `domain`, `telemetry`, `logging`) are written in Kotlin and compiled to multiple targets through KMP.

This is the answer to "how do we share the platform-independent logic." It is not the answer to "how do we build the UI" or "how do we access platform APIs" — those are decided separately (see ADR-002).

---

## Consequences

**Positive:**

- Single source of truth for parsing, validation, and protocol implementations. Bug fixes and specification updates apply to every target simultaneously.
- Eliminates drift between platform implementations of the same logic.
- Reduces maintenance burden compared to duplication.
- Kotlin is well-suited to the kind of work this SDK does (sealed types, value classes, pattern matching, coroutines). The language itself is a good fit.
- KMP supports the future targets we have in mind (JVM backend, web via JS/Wasm, native desktop) without architectural changes.

**Negative:**

- Adds a build-tool dependency on KMP, including its evolving toolchain and versioning quirks.
- Consumers integrating on iOS receive an artifact built from Kotlin code rather than a hand-written Swift library; the public API is hand-tuned for Swift idiom but the underlying implementation is Kotlin/Native.
- Some Kotlin features (notably coroutines) translate imperfectly to Swift; explicit interop work is required for clean idiomatic API surfaces on iOS.
- Onboarding contributors with no Kotlin background requires more learning than a duplicate-per-platform approach would.

**Neutral:**

- The architecture cleanly separates shared logic (KMP) from native UI and platform I/O (per platform). This decision is bounded — KMP applies only to the shared core, not to everything.

---

## Alternatives Considered

**Duplicate per platform.** Write the parser in Kotlin for Android, Swift for iOS, TypeScript for web, etc. Rejected because the maintenance burden is prohibitive: every bug fix, every ICAO specification update, every new format requires N parallel implementations. Drift is inevitable.

**Rust with platform bindings.** Use Rust as the shared logic layer with platform-specific bindings. Rejected because the binding overhead is significant for both Android (JNI) and iOS, the toolchain complexity is higher, and the team's familiarity with Kotlin made it the more practical choice. Rust would be a defensible alternative for a different team composition.

**C++ with platform bindings.** Considered briefly. Rejected on similar grounds: high binding overhead, lower team familiarity, and worse fit for the SDK's domain (protocol parsing, structured data) than a higher-level language.

---

## Related Decisions

- ADR-002 — native UI per platform; addresses the question "what about UI" that this ADR explicitly does not answer
- ADR-003 — modular architecture; KMP makes the shared modules cleanly separable

---

## Related Documents

- `architecture.md` — describes the layered architecture this decision enables
- `principles.md` — Principle 8 (Native fit over cross-platform purity); KMP is used where it shines, not as a universal answer
