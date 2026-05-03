# ADR-003: Modular architecture from day one

**Status:** Accepted

---

## Context

A common pattern in early-stage projects is to start with a single module and split it later when modularity becomes clearly necessary. This is the "monolith first" approach: keep things simple early, refactor when needed.

The opposite pattern — modular from day one — has its own costs: more upfront design work, more files, the discipline to maintain interfaces, and the risk of over-modularizing for needs that never materialize.

A decision was needed about which approach to take, because the answer shapes how every subsequent piece of work is structured and what consumers see when they integrate the SDK.

---

## Decision

The project is structured as multiple modules from the first release, with explicit dependencies between them and clear public API surfaces per module.

The decision is operationalized through Principle 11 (Internal packages first, standalone modules when justified): new features start as internal packages with clean public API boundaries inside existing modules. They are promoted to standalone modules only when independent reuse, evolution, testing, ownership, shipping, or optional inclusion clearly applies.

This balances the two extremes: avoiding the monolith that grows without internal structure, while also avoiding the over-modularized project where every concept is a separate artifact regardless of need.

---

## Consequences

**Positive:**

- Consumers can include only the modules they need (e.g., `mrz-core` alone for a backend validation service, without pulling in camera or NFC modules).
- Internal complexity within a module is bounded; cross-module complexity is forced to be explicit through public API contracts.
- Replacing a module's implementation is mechanical when its dependencies are bounded — for example, swapping an OCR engine in `mrz-camera-android` does not affect `mrz-core` consumers.
- The dependency graph is documented and verifiable; circular dependencies are forbidden and detectable at build time.
- The architecture supports future targets (backend, web, desktop) cleanly because the I/O layer is already separated from the core logic.

**Negative:**

- More upfront thought is required when adding new functionality (where does it go, what depends on what, what is its public API).
- More files and more module-level documentation than a monolith would require.
- The "Internal packages first" rule requires discipline; without it, the project drifts toward over-modularization.

**Neutral:**

- Module boundaries can be revisited. If a module proves to be the wrong factoring, it can be refactored. The cost of getting it slightly wrong is real but bounded.

---

## Alternatives Considered

**Monolith first, split when necessary.** Rejected because the SDK's domain has obvious natural seams (MRZ logic vs chip logic, core logic vs platform I/O, headless vs UI). Building a monolith and refactoring later means doing the design work anyway, just with more code already written against the wrong structure.

**One module per platform.** Considered as a simpler alternative ("everything for Android in one place"). Rejected because it conflates the platform-independent logic (parsing, validation, protocol implementation) with platform-specific concerns (camera, NFC, UI). The result is an Android module that duplicates an iOS module, with all the drift problems duplication creates.

**Maximum modularity from day one.** Considered as the opposite extreme: every concept (parsing, generation, validation, each lookup table, each transliteration profile) as its own module. Rejected because the cost of module-level overhead per concept is not justified for concepts that always travel together. Principle 11 explicitly addresses this — modules require justification.

---

## Related Decisions

- ADR-001 — Kotlin Multiplatform; the modular structure of shared core logic is enabled by KMP's module model
- ADR-002 — native UI per platform; UI modules are leaves in the modular graph, fully optional

---

## Related Documents

- `architecture.md` — the module list, dependency graph, and layer boundaries
- `principles.md` — Principle 3 (Modular architecture, not monolith) and Principle 11 (Internal packages first)
- `conventions.md` — naming and structuring conventions for modules
