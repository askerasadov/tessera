# ADR-012: Recognition-bearing value classes live with their lookup tables

**Status:** Accepted

> **Note on naming:** This ADR was written when the shared-types module was named `domain`. That module has since been renamed to `types` (per ADR-016's executed Follow-up Cleanup). All references to "the `domain` module" throughout this ADR's reasoning should be read as "the `types` module" — the substance of the decision (which classes live where, why, and the dependency-graph constraints) is unchanged.

---

## Context

Several types in the SDK wrap raw MRZ codes and expose recognition state alongside the raw value. The two most prominent are `CountryCode` (wraps a three-letter code, exposes whether the code is recognized and the country's full name) and `DocumentType` (wraps a one or two-character code, exposes whether the code is recognized and a categorical interpretation).

The data model document (`docs/features/mrz-data-model.md`) describes these types as part of the MRZ data model. The architecture document (`docs/architecture.md`) names `domain` as the module containing "shared types and vocabulary used across modules: document type enumerations, country and nationality codes, error taxonomy base classes, and common data structures."

The two documents pull in different directions. The data model says these types expose recognition state. Recognition state requires consulting a lookup table. The lookup tables live in `mrz-core` (per `docs/features/lookup-tables.md`). The architecture is explicit that `domain` depends on nothing else — so `domain` cannot consult a table that lives in `mrz-core`.

A decision was needed about where these recognition-bearing value classes live, because the answer determines the shape of the very first code commits and sets a precedent for similar types added later.

---

## Decision

Value classes whose contract requires consulting a runtime data structure (a lookup table) live in the same module as that data structure. They are not hoisted into `domain`.

Concretely:

- `CountryCode` and `DocumentType` value classes live in `mrz-core`, next to the lookup tables they consult.
- `domain` holds only types that are self-contained and table-free: format enumerations (`MrzFormat`), categorical enumerations (`Sex`, `DocumentCategory`, `CountryCodeCategory`), shared field identifiers (`MrzField`), the sealed roots of the error taxonomy (`MrzError`, `MrzValidationError`, `MrzWarning`), and common data structures that have no runtime data dependency.
- The architecture document is updated so the `domain` module description is unambiguous about this split.

If a type later needs to be reachable from a sibling module (for example, when `emrtd-core` begins handling country codes from chip data), it is promoted to `domain` per Principle 11's promotion rules at that point — not preemptively now.

---

## Consequences

**Positive:**

- The type stays in one piece. There is no thin shell in `domain` paired with extension functions in `mrz-core` to recover the recognition contract — the type and its data live together.
- The `domain` module remains genuinely tiny and table-free, preserving its "depends on nothing else" property literally rather than nominally.
- The decision is consistent with Principle 11 (Internal packages first): types live in their natural host module until a concrete sibling-module use case justifies promotion. Speculative promotion is rejected.
- The decision is consistent with Principle 2 (Don't add infrastructure before it earns its keep): the cost of a future migration is real but bounded; the cost of pre-promoting is paid every release.
- The reasoning extends naturally to types not yet imagined. Any future value class whose contract requires a lookup table follows the same rule.

**Negative:**

- When `emrtd-core` eventually needs `CountryCode` (chip data groups encode nationality), `CountryCode` will need to be promoted to `domain`. That promotion is a breaking package change for any consumer that has imported `io.lightine.tessera.mrz.recognition.CountryCode` directly. Per `docs/versioning.md`, this requires a deprecation cycle — the old location stays as a typealias for at least one minor version.
- Code in `domain` that wants to mention country codes (for example, an error variant that takes a country code as context) cannot use the value class directly. It must use the raw string, or the eventual promotion needs to happen earlier than this ADR currently anticipates. This is a watch item for the error taxonomy as it grows.
- The ADR fixes a reading of architecture.md that the original text did not explicitly commit to. Future contributors who read the original wording differently may push back. The architecture.md update (see below) is intended to remove that ambiguity, not to relitigate it.

**Neutral:**

- The decision can be revisited. If two or more sibling modules end up needing the same recognition-bearing type before any release ships, promoting it to `domain` is mechanical and the reasoning above does not block the move — it only argues against doing it speculatively.

---

## Alternatives Considered

**Put `CountryCode` and `DocumentType` in `domain` as full value classes (with recognition state).** Rejected because `domain` cannot consult the lookup tables, which live in `mrz-core`. Implementing this would require either inverting the dependency graph (forbidden by `architecture.md`), bundling the lookup tables into `domain` (which would force every consumer of `domain` to carry the table data even if they never read MRZs), or making the recognition contract lazy and platform-injected (overcomplicated for the problem).

**Put `CountryCode` and `DocumentType` in `domain` as thin shells, with recognition contract added by extension functions in `mrz-core`.** Rejected because the type is now split across two modules. A consumer holding a `CountryCode` cannot reach the recognition contract without an additional import from `mrz-core`, which defeats the value of having the type in `domain` in the first place. The split is also ergonomically poor: `countryCode.isRecognized` works in some files but not others depending on imports.

**Pre-promote to `domain` because `emrtd-core` is "obviously" going to need country codes eventually.** Rejected per Principle 11 (Internal packages first) and Principle 2 (Don't add infrastructure before it earns its keep). Future use is not present use; the rule explicitly rejects promotion on speculation alone.

**Treat the architecture.md text "country and nationality codes" as the authoritative reading and force the value classes into `domain`.** Rejected because `architecture.md` itself states that "When this document conflicts with a principle, the principle wins and this document is updated." Principle 11 is the principle in tension here, and the resolution is to update the architecture document rather than violate Principle 11.

---

## Related Decisions

- ADR-003 — modular architecture from day one. The dependency graph this ADR honors is the one ADR-003 established.

---

## Related Documents

- `architecture.md` — module list and dependency graph. The `domain` module description is updated by this ADR to remove ambiguity.
- `principles.md` — Principle 2 (Don't add infrastructure before it earns its keep) and Principle 11 (Internal packages first), which together justify the decision.
- `mrz-data-model.md` — describes `CountryCode` and `DocumentType` as recognition-bearing value classes.
- `lookup-tables.md` — describes the lookup tables these value classes consult.
- `versioning.md` — defines the deprecation cycle that applies if `CountryCode` is later promoted to `domain`.
