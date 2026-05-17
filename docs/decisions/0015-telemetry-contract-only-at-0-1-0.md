# ADR-015: Telemetry interface ships as contract-only at 0.1.0 with an open event hierarchy

**Status:** Accepted

---

## Context

[`docs/scope.md`](../scope.md) (release 0.1.0 entry) and [`docs/architecture.md`](../architecture.md) (the `telemetry` module description and the dependency graph) commit the SDK to a **pluggable telemetry interface** in 0.1.0: a contract that consumers may implement to receive diagnostic events about SDK operation, a default no-op implementation, and utilities for redaction. There is no built-in telemetry destination and no phone-home behavior — the SDK never decides where events go.

Two facts about the 0.1.0 release shape constrain the slice:

1. **No 0.1.0 module emits events.** The architecture dependency graph at [`docs/architecture.md`](../architecture.md) shows `telemetry` as depended on by `mrz-camera-{platform}` and `emrtd-nfc-{platform}` only — not by `mrz-core` or `emrtd-core`. The first ships in 0.2.0; the second in 0.6.0. The MRZ parsing, validation, and generation work that constitutes 0.1.0's substance does not call into telemetry.
2. **[ADR-007](0007-strict-backward-compat-from-0x.md) locks the public surface at 0.1.0.** Any public type or signature shipped in 0.1.0 cannot break across the 0.x line. Adding a new variant to a `sealed` hierarchy is a breaking change under exhaustive matching; the strict-backcompat policy does not include a carve-out window.

The combination is the awkward part. The scope commits to telemetry in 0.1.0, the dependency graph says no 0.1.0 module calls it, and the backward-compatibility rule means the surface chosen now cannot be replaced — only deprecated and added to. The decision recorded here is how the slice resolves those three constraints.

---

## Decision

The 0.1.0 release ships the **telemetry contract and its supporting machinery, with no production call sites inside 0.1.0 SDK modules**. The first real callers are the camera-based reading module (0.2.0) and the NFC chip reading module (0.6.0). Specifically:

- **Public surface published in 0.1.0:**
  - `TelemetrySink` — interface with a single `record(event: TelemetryEvent)` method. Consumers implement it.
  - `TelemetryEvent` — **open interface** with a single `name: String` property. Future event types implement it.
  - `NoOpTelemetrySink` — object implementing `TelemetrySink`; the default sink.
  - `TelemetrySinkRegistry` — singleton object holding the currently-registered sink. Methods: `register(sink)`, `reset()`, `current`.
  - `Redaction` — object with utility functions for masking sensitive content in event payloads. 0.1.0 ships one function, `redactMrzLine(line)`; more helpers are added when the events that need them ship.

- **`TelemetryEvent` is an open interface, not a sealed hierarchy.** Future event types are added in the releases of the modules that emit them (camera in 0.2.0, NFC in 0.6.0, possibly others). Open-interface additions are non-breaking by definition — consumers cannot exhaustively pattern-match on an open interface, so adding a new implementation does not break their code. This diverges from the project's existing sealed-result-type pattern (`ParseResult`, `TransliterationResult`); the divergence is justified because telemetry events have a different consumer contract than result types: consumers route events to their observability backend, they do not pattern-match on them.

- **Registry is a singleton mutable object** matching the precedent set by `TransliterationProfileRegistry`. The documented contract is "register at startup before concurrent use." The registered sink may be called from any thread once SDK modules begin emitting events, so consumer implementations must be thread-safe. The 0.1.0 release does not enforce this — there are no callers — but the contract is documented now so it is part of the locked surface.

- **The "nothing happens in 0.1.0" property is documented in the public KDoc on `TelemetrySink`, in [`docs/features/telemetry.md`](../features/telemetry.md), and in the `[Unreleased]` CHANGELOG entry.** This is honest disclosure per [Principle 4](../principles.md). A consumer who registers a sink in 0.1.0 and observes no events is encountering documented behavior, not a bug.

- **0.1.0 has no emitted events.** The `record(event)` method is callable in principle but no concrete `TelemetryEvent` implementation ships in 0.1.0. The first concrete event types ship in 0.2.0 alongside the camera module; consumers can implement custom `TelemetryEvent` types if they want to test their sink wiring, but that is not part of the SDK's contract.

---

## Consequences

**Positive:**

- The 0.1.0 scope commitment is honored without overshooting. The contract is locked under ADR-007; the implementation slice is small and focused; consumers who depend on the surface for forward-compatibility planning have a stable type to reference. ([Principle 9](../principles.md) — forward-compatible API.)
- The open-interface choice means 0.2.0 and 0.6.0 can add their event types as additive changes, with no need for an ADR-007 carve-out and no deprecation cycle for events introduced post-0.1.0. ([ADR-007](0007-strict-backward-compat-from-0x.md) holds without exception.)
- Documenting "no events emitted in 0.1.0" explicitly, in every consumer-visible place, makes the empty-by-design property a feature rather than a surprise. ([Principle 4](../principles.md) — honest about what we know.)
- The singleton-registry pattern matches `TransliterationProfileRegistry` and avoids inventing a new wiring model. Consumers who learn one of the two registries understand the other. ([Principle 2](../principles.md) — logical defensibility through consistency.)
- The redaction utility surface stays minimal — one function — and grows alongside the events that need redaction. Speculating on redaction helpers for event types that do not yet exist would lock the wrong surface under ADR-007.

**Negative:**

- The contract is locked before any real caller exists. If the eventual camera or NFC modules need a different sink shape (e.g., per-event sinks, structured context propagation, transactional batching), the 0.1.0 surface either has to be deprecated-and-replaced or the new requirement has to be expressed through a new abstraction layered on top of the locked `TelemetrySink`. This is the standard cost of strict-backcompat-from-0.x and is acknowledged in [ADR-007](0007-strict-backward-compat-from-0x.md).
- The open-interface pattern diverges from `ParseResult`, `TransliterationResult`, `GenerationResult`, and the rest of the project's sealed-result types. Future contributors reading the codebase may notice the inconsistency and wonder why. This ADR is the answer; it is linked from `docs/features/telemetry.md` and from the KDoc on `TelemetryEvent`.
- Registering a `TelemetrySink` in a 0.1.0-only application has no observable effect. Consumers might wire up the SDK, register a sink, and conclude the SDK is broken before reading the documentation. The mitigation is the explicit "no events in 0.1.0" disclosure in every relevant doc and KDoc; the trade-off (a less-than-ideal first impression for the small set of consumers who experiment with the sink in isolation) is preferred over either deferring the contract past 0.1.0 (which violates scope) or shipping speculative events (which would lock the wrong surface).

**Neutral:**

- The choice to expose redaction as utility functions rather than as a `Redactor` interface reflects 0.1.0's tiny surface. If future events make the policy complex enough to warrant an interface, a `Redactor` abstraction can be added additively in a later release; the utility functions are stable building blocks regardless.
- Thread-safety is documented as a consumer responsibility, not enforced. This matches the transliteration registry's stance and reflects the absence of 0.1.0 callers — formal threading guarantees can be added later if needed without breaking the locked surface.

---

## Alternatives Considered

**Alternative A — Sealed `TelemetryEvent` hierarchy with subtypes added in minor releases.** The project's existing result types (`ParseResult`, `TransliterationResult`) are sealed; matching the pattern would feel natural. Rejected because adding a sealed subtype after 0.1.0 is a breaking change under ADR-007's strict reading: consumer code that pattern-matches exhaustively on the sealed root stops compiling when a new subtype lands. Carving out an exception for `TelemetryEvent` specifically would weaken ADR-007 in exchange for a stylistic match the consumer contract does not actually require — consumers route events, they do not pattern-match.

**Alternative B — Sealed hierarchy with all event types committed in 0.1.0.** Avoids the additive-breakage problem by declaring every event type the SDK will ever emit upfront. Rejected because 0.1.0 does not know what events camera (0.2.0) and NFC (0.6.0) need. Designing those event types now would be speculative; under ADR-007 the speculation would be locked into the public surface forever. The alternative trades a real backward-compatibility risk for a worse one.

**Alternative C — Extend the architecture dependency graph so `mrz-core` (and `emrtd-core`) emit telemetry too.** This would give 0.1.0 a real caller and let the event types be designed against actual emission sites. Rejected because it is a substantive architectural change beyond the 0.1.0 scope: it changes the module dependency graph in `architecture.md` and requires designing the MRZ event taxonomy (parse outcomes, validation results, generation events) that was deliberately not in the 0.1.0 plan. The Pre-Release Tech-Stack Review (2026-05-17) did not flag this as needed; reopening it now would be re-scoping the release.

**Alternative D — Defer the telemetry interface to 0.2.0 where the first real consumer lives.** Conceptually cleanest: ship the contract together with its first caller. Rejected because [`docs/scope.md`](../scope.md) explicitly commits the pluggable telemetry interface as part of 0.1.0, and the Pre-Release Tech-Stack Review (2026-05-17) confirmed the commitment. Deferring would re-litigate a settled decision. The contract-only-with-honest-disclosure approach respects the scope commitment without overcommitting the design.

**Alternative E — Generic event with `name: String` plus `attributes: Map<String, Any?>`.** A single concrete event class that carries arbitrary payloads as a string-keyed map. Rejected because it loses type information at the consumer boundary: every event becomes a stringly-typed lookup, and the SDK's principle of structured-over-opaque ([Principle 5](../principles.md) — transparency) argues against it. The open-interface approach lets each future event type carry its own typed fields while still allowing consumers to handle them generically through the `name` property when they choose to.

---

## Related Decisions

- **[ADR-007 — Strict backward compatibility from 0.x.](0007-strict-backward-compat-from-0x.md)** The reason the event hierarchy must be open rather than sealed: additive sealed subtypes are breaking under exhaustive matching, and ADR-007 does not permit a carve-out.
- **[ADR-009 — Per-state transliteration profiles, never inferred.](0009-transliteration-profiles.md)** The `TransliterationProfileRegistry` singleton precedent that `TelemetrySinkRegistry` follows.
- **[ADR-003 — Modular architecture from day one.](0003-modular-architecture.md)** The dependency-graph constraint (`telemetry` as a cross-cutting module depended on by I/O modules, not by core logic) that produces the "no 0.1.0 caller" situation in the first place.

---

## Related Documents

- [`docs/scope.md`](../scope.md) — release 0.1.0 commits the pluggable telemetry interface; the contract-only framing satisfies that commitment.
- [`docs/architecture.md`](../architecture.md) — the `telemetry` module description and dependency graph that establishes who calls into telemetry and who does not.
- [`docs/features/telemetry.md`](../features/telemetry.md) — feature document; the consumer-facing description of the surface this ADR locks.
- [`docs/principles.md`](../principles.md) — Principles 2, 4, 5, 9 referenced above.
- [`CLAUDE.md`](../../CLAUDE.md) — the Pre-Release Tech-Stack Review rule under which this slice was scheduled.
