# ADR-010: Apache 2.0 license at public release

**Status:** Accepted

---

## Context

The project will be released as open source at the 1.0.0 milestone. Open source requires choosing a license. The license shapes:

- What consumers can do with the code (use, modify, redistribute, sublicense)
- What contributors implicitly grant when they submit code
- Compatibility with other open source projects that may want to incorporate or depend on this SDK
- The legal posture for any commercial entity using the SDK

The major candidates for a developer-tools project of this kind are: MIT, BSD (2-clause or 3-clause), Apache 2.0, MPL 2.0, and various copyleft licenses (LGPL, GPL).

A decision was needed before public release.

---

## Decision

The project is released under the Apache License 2.0 at the 1.0.0 public release.

---

## Consequences

**Positive:**

- Apache 2.0 includes an explicit patent grant: contributors grant a patent license to users for any patents they hold that are infringed by their contributions. This is meaningful protection for both users and contributors.
- Apache 2.0 is widely understood and accepted in commercial contexts. Enterprise consumers can adopt the SDK without legal review surprises.
- Compatible with most other open source licenses (it is one-way compatible with GPLv3 but not GPLv2, which is rarely a practical issue for an SDK).
- The license requires preserving copyright notices but does not impose copyleft. Consumers who modify the SDK for their own use are not required to release their modifications.
- Apache 2.0 is the de facto standard for many infrastructure-level open source projects, including projects in the Kotlin and Android ecosystems.

**Negative:**

- More verbose than MIT or BSD. License headers and the LICENSE file are longer.
- Some integrators may prefer simpler licenses (MIT, BSD); Apache 2.0 may require slightly more attention to the NOTICE file mechanism.
- Not compatible with GPLv2-only projects that might want to incorporate the SDK directly.

**Neutral:**

- The license does not constrain how the SDK evolves or what features it includes. License choice is independent of feature scope and design decisions.

---

## Alternatives Considered

**MIT.** Considered. MIT is shorter, simpler, and very widely accepted. Rejected primarily because of the lack of an explicit patent grant. For an SDK that touches cryptographic protocols and identity document handling — areas where patent landscapes can be murky — the explicit patent grant in Apache 2.0 provides meaningful protection.

**BSD 3-Clause.** Considered. Similar properties to MIT (permissive, simple) with the addition of a non-endorsement clause. Rejected for the same reason as MIT: no explicit patent grant.

**MPL 2.0.** Considered. File-level copyleft (modifications to MPL files must be shared, but the SDK can be used in larger proprietary works). Rejected because the file-level copyleft is more friction than the project benefits from. The audience for this SDK includes commercial integrators who would benefit from the simpler permissive structure.

**LGPL or GPL.** Not seriously considered. The strong copyleft of these licenses is incompatible with the SDK's intended use as a permissively-integrable component in consumer applications, including proprietary ones.

**Dual licensing.** Considered briefly (e.g., Apache 2.0 plus a commercial license for proprietary derivatives). Rejected because the project is fully permissive — there is no commercial restriction worth carving out — and dual licensing adds significant legal and operational complexity without clear benefit.

---

## Related Decisions

- ADR-011 — open source at public release. This ADR specifies the license; ADR-011 specifies the broader stance.

---

## Related Documents

- `scope.md` — the 1.0.0 release section notes the open-sourcing milestone
- `versioning.md` — establishes 1.0.0 as the point of public stability commitments and open-sourcing
