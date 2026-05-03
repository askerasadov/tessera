# ADR-011: Open source at public release

**Status:** Accepted

---

## Context

The project starts as an internal effort, developed in private during the 0.x phase. A decision was needed about what happens at the 1.0.0 milestone:

- Continue as a closed-source project, distributed only to known consumers?
- Release as open source with a permissive license?
- Adopt a dual model (open core, commercial extensions)?

This shapes the project's audience, contribution model, business relationship with integrators, and long-term sustainability.

---

## Decision

The project is released as open source at the 1.0.0 milestone. There is no commercial subscription or proprietary extension. The full SDK, all features, all platforms, all future enhancements are released under the open source license (see ADR-010 for the license choice).

---

## Consequences

**Positive:**

- The SDK becomes available to a broad audience: commercial consumers, government integrators, hobbyists, researchers, students. Anyone who needs to read identity documents in a principled way can use it.
- Open source signals confidence: the SDK can withstand scrutiny of its implementation, its security posture, and its design choices.
- A community of users may contribute improvements, additional country profiles, additional document type support, and bug fixes.
- The reader-not-oracle stance (ADR-004) is more credible when the source is auditable. Consumers can verify that the SDK does what it claims to do.
- No commercial gating means no consumers are excluded based on their ability or willingness to pay.

**Negative:**

- The project does not generate direct revenue. Sustainability depends on volunteer effort, sponsorship, or alignment with a sponsor's business interest.
- Open source requires governance: how decisions are made, how contributions are reviewed, how releases are cut, how issues are handled. This adds operational overhead beyond closed-source development.
- The maintainers carry responsibility for the broader user base, not just immediate consumers. A security issue or breaking change affects everyone.
- Open source does not automatically produce a community. Many open source projects have one or two maintainers and few external contributors; assuming community contribution is a known failure mode.

**Neutral:**

- The decision is independent of the SDK's design. Whether open or closed, the SDK would have the same architecture, the same principles, and the same scope. Open source is a distribution and licensing choice.

---

## Alternatives Considered

**Closed source distributed to known consumers.** Considered. Practical for a focused initial deployment but limits the audience permanently. Rejected because the SDK's value increases significantly with broader adoption (more eyes, more bug reports, more profile contributions, more validation against diverse real-world inputs).

**Open core with commercial extensions.** Considered. The SDK core would be open source; certain features (e.g., advanced verification, dashboards, support tooling) would be proprietary or commercially licensed. Rejected because the SDK does not have a natural "core vs extension" split that justifies it. The reader-not-oracle stance means the SDK is intentionally not in the verification or business-rules space; there is no "extension layer" that consumers would reasonably pay for as part of the SDK itself.

**Dual licensing (e.g., Apache for non-commercial, commercial license for commercial use).** Rejected for the same reason discussed in ADR-010: no commercial restriction worth carving out, and dual licensing adds operational complexity disproportionate to the benefit.

**Source-available without an OSI-approved license.** Considered briefly. Rejected because it provides few of the practical benefits of true open source (community contributions, broad adoption, auditability is conditional) and many of the costs.

---

## Related Decisions

- ADR-010 — Apache 2.0 license. ADR-011 establishes that the project is open source; ADR-010 specifies the license.
- ADR-007 — strict backward compatibility from 0.x. Open source consumers have stronger expectations of stability; the strict-from-0.x policy aligns with this.

---

## Related Documents

- `scope.md` — the 1.0.0 release section
- `versioning.md` — versioning policy that applies to public releases
