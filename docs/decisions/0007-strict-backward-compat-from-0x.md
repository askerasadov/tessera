# ADR-007: Strict backward compatibility from 0.x (Position A)

**Status:** Accepted

---

## Context

Semantic Versioning (SemVer) defines clear backward-compatibility rules for versions 1.0.0 and above: within a single MAJOR version, public APIs do not break. The convention for pre-1.0 versions (the 0.x line) is less clear and varies across projects.

Two common positions exist:

- **Position A (strict):** 0.x follows the same backward-compatibility rules as 1.0+. Within the 0.x line, public APIs do not break. The 1.0 release marks a milestone (often "we're publicly committing to stability") but does not mark the moment compatibility starts mattering.
- **Position B (loose):** 0.x signals "API may change without notice." Breaking changes are allowed in any 0.x release; consumers using 0.x accept this as the cost of using pre-release software. The 1.0 release is when compatibility commitments begin.

Position B is the more common OSS convention. Position A is stricter. A decision was needed before the 0.1.0 release.

---

## Decision

The project follows Position A: strict backward compatibility from 0.x onward. Within the 0.x line, public APIs do not break. Breaking changes during the 0.x phase, when they occur, are MAJOR version bumps (a true breaking change in the 0.x line produces a new MAJOR version).

In practice, the expected pattern is: 0.x development proceeds through MINOR additions only. Breaking changes are deferred and bundled into the 1.0.0 release, which marks the public-stability commitment and the open-sourcing of the project.

---

## Consequences

**Positive:**

- Forces careful API design from the first release. Difficult API decisions cannot be deferred to a hypothetical 1.0; they must be made now.
- Builds consumer trust early, including for the internal-only phase. Consumers (internal or external) integrating against 0.x can upgrade across MINOR versions without code changes.
- Makes the 0.x → 1.0 transition meaningful as a stability and maturity milestone, not as the moment compatibility starts mattering. The 1.0 release marks "we now stand behind the API publicly," not "now you can rely on us not breaking things."
- Aligns with Principle 9 (Forward-compatible API): public surfaces are stable contracts.

**Negative:**

- Less freedom during early development. Mistakes in API design are harder to correct without a deprecation cycle.
- Slower iteration on the API surface. Experimental APIs that turn out to be wrong cannot simply be replaced; they must be deprecated and the replacement must be additive.
- More upfront design work per feature. Each public API must be considered carefully before it ships, because removing it later requires the deprecation cycle.

**Neutral:**

- The decision is consistent with Principle 4 (Honest about what we know): version numbers convey real information. A MINOR bump means non-breaking; a MAJOR bump means breaking. Both pre-1.0 and post-1.0.

---

## Alternatives Considered

**Position B (loose 0.x).** The conventional choice. Rejected because:
- It defers difficult API decisions to a future moment that may never come, or may come under pressure.
- The 0.x → 1.0 transition becomes an artificial milestone that requires "freezing" the API at a specific point.
- Consumers using 0.x must defensively code against arbitrary changes, which encourages using stable forks or vendoring.
- The "0.x means anything goes" convention encourages sloppy API design that becomes locked in by 1.0 without ever having been deliberately reviewed.

**Hybrid (loose for 0.0.x, strict for 0.1.0+).** Considered as a middle ground: very early versions can break freely, but once we hit 0.1.0 we commit. Rejected because the boundary is arbitrary and the discipline of strict compatibility from the start is itself valuable.

**Position A but with a documented exception window.** Considered as a way to handle truly egregious mistakes that must be corrected. Rejected because the exception becomes a loophole. If a mistake is severe enough to warrant breaking, the deprecation cycle is the right way to handle it; bypass mechanisms erode the commitment.

---

## Related Decisions

- ADR-005 — no verification hooks in initial release. Position A means we are careful not to ship hooks now whose shape we are unsure about, because changing them later is breaking.
- ADR-006 — no `isValid` boolean. Position A means we don't ship a property whose semantics we might want to refine later.

---

## Related Documents

- `versioning.md` — the full versioning policy, including this decision
- `principles.md` — Principle 9 (Forward-compatible API) and Principle 4 (Honest about what we know)
