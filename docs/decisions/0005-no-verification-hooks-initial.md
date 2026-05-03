# ADR-005: No verification hooks in initial release

**Status:** Accepted

---

## Context

Beyond MRZ extraction and validation, identity document workflows often involve verification: cryptographic signature verification of NFC chip data, comparison against external registries (lost or stolen documents, sanctions lists), face matching against the document photograph, and so on.

Several design questions arose:

- Should the SDK include built-in verification capabilities?
- Should it provide hooks for consumers to plug in their own verification?
- Should it stay out of verification entirely in the initial release?

The decision matters because it shapes the SDK's surface area, its dependency footprint, and the boundary between SDK responsibility and consumer responsibility.

---

## Decision

The initial release does not include verification capabilities or hooks for verification. The SDK extracts data and surfaces validation results; consumers perform any verification themselves using their own infrastructure.

Verification capabilities may be added in future releases as additive features, but they are explicitly not part of the initial scope.

---

## Consequences

**Positive:**

- The initial release has a clean, bounded scope. The SDK does what it does well; verification is a separate concern handled by separate systems.
- Consumers are not constrained by the SDK's choices about verification mechanisms. A consumer with their own backend verification API uses it directly without adapting to an SDK contract.
- The SDK has no dependencies on cryptographic infrastructure beyond what is needed for chip access protocols (BAC and PACE). Trust anchor management, signature verification, certificate validation, and so on are not in the dependency graph.
- The initial release ships sooner than it would if verification were in scope.

**Negative:**

- Consumers must integrate verification themselves. Common verification patterns are not codified in the SDK; each consumer reinvents.
- The SDK cannot offer convenience for consumers who want a "complete" identity verification flow without additional integration work.
- A future addition of verification hooks must be designed carefully to be additive (not breaking existing consumers).

**Neutral:**

- The reader-not-oracle stance (ADR-004) makes this decision easier to live with: the SDK was never going to *make* verification decisions; the question was only whether it would *facilitate* them.

---

## Alternatives Considered

**Built-in verification with bundled implementations.** Reject. Bundles cryptographic logic, trust anchor management, and external service integrations into the SDK. Significantly increases scope, dependency footprint, and surface area. Tightly couples the SDK to specific verification approaches that may not match consumer needs.

**Verification hooks with consumer-provided implementations.** Considered carefully. The pattern would be: SDK defines contracts (e.g., `ChipSignatureVerifier`, `DocumentRegistryChecker`), consumer provides implementations, SDK invokes them at appropriate points. Rejected for the initial release for two reasons:
- Designing the contracts well requires more clarity about consumer use cases than we currently have. Premature contracts often lock in patterns that don't fit real needs.
- Consumers without verification needs would still see the contracts in the API and have to understand they can ignore them. Cleaner to add hooks later when their shape is informed by real usage.

**Verification as a separate optional module.** Considered as a future enhancement (post-1.0). Not rejected — this is a likely path for adding verification capabilities later. But not in scope for the initial release.

---

## Related Decisions

- ADR-004 — reader, not oracle. Verification is a trust decision; the SDK does not make trust decisions; therefore the SDK does not need verification built in.
- ADR-007 — strict backward compatibility from 0.x. Means future verification additions must be careful not to break existing consumers.

---

## Related Documents

- `scope.md` — the "Beyond 1.0" section lists cryptographic chip signature verification as a future capability
- `reading-risks.md` — describes what each reading method does and does not establish; verification is one of the things consumers may want to layer on top
- `principles.md` — Principle 1 (Reader, not oracle) and Principle 2 (Logical defensibility — fewer assumptions about future needs)
