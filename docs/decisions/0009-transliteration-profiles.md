# ADR-009: Per-state transliteration profiles, never inferred

**Status:** Accepted

---

## Context

The MRZ alphabet is restricted (uppercase A-Z, digits, filler character). Real-world names contain characters outside this set: diacritics, characters from non-Latin scripts, ligatures, and so on. Generating an MRZ from such names requires transliteration — converting these characters into MRZ-compatible representations.

ICAO Doc 9303 Part 3 Section 6 (Annex G) provides default transliteration recommendations covering Latin-1 Supplement (U+00C0-00DE), Latin Extended-A (U+0100-017D), and U+1E9E. Issuing states often define their own conventions for characters outside this coverage, and the same character may be transliterated differently depending on the state.

The Latin schwa character (`Ə` U+018F / `ə` U+0259) is the clearest example. Schwa lives in Latin Extended-B (U+0180-024F) and is **not in Annex G's table at all** — Annex G provides no recommendation for it. Each issuing state that needs to encode schwa in an MRZ therefore makes its own choice. The convention for the issuing state with ISO 3166-1 alpha-3 code `AZE` is `Ə → A`, derivable via a two-step chain of authoritative standards:

1. **BGN/PCGN 1993 Agreement** (UK government romanization system for the AZE alphabet, 2022 revision) Note 1: *"The special letter Ə, ə known as schwa, should be reproduced in that form whenever encountered. In those instances when it cannot be reproduced, however, the letter Ä ä may be substituted for it."* In the MRZ alphabet, schwa cannot be reproduced.
2. **ICAO Doc 9303 Part 3 Annex G** under the no-expansion convention: `Ä → A`.

Chained: `Ə → Ä (BGN/PCGN fallback) → A (ICAO no-expansion)`. This matches observed practice in AZE-issued passports. The SDK's `AzeTransliterationProfile` applies this override; the ICAO default profile (`IcaoDefaultTransliterationProfile`) does not include schwa at all, matching Annex G's actual scope (schwa falls through to the filler character `<` in the ICAO default).

A decision was needed about how the SDK handles this divergence:

- A single global table per the ICAO defaults?
- A profile system with per-state mappings?
- Inferred profile selection based on issuing state?

---

## Decision

The SDK uses a profile-based architecture: a `TransliterationProfile` defines the rules for a specific issuing context. The SDK ships with the ICAO default profile and at least one country-specific profile in the initial release; consumers can register their own profiles.

The generator never infers which profile to use. The consumer must specify a profile explicitly, or the SDK refuses to transliterate (returning a typed error).

---

## Consequences

**Positive:**

- Honors Principle 1 (Reader, not oracle): the SDK does not guess locale based on issuing state, holder nationality, or any other contextual signal. The consumer has the context; the SDK does not.
- Captures real-world divergence accurately. A single global table would produce wrong MRZs for states whose conventions differ from ICAO defaults.
- The profile system is open: consumers can register their own profiles for issuing states the SDK has not yet shipped a profile for. They do not need to fork.
- The decision is testable: given an input and a profile, the output is deterministic and inspectable.

**Negative:**

- Consumers must know which profile to use. A consumer generating an MRZ for a passport must specify the profile for the issuing state; if they get it wrong, the MRZ may not match what the state actually issues.
- More API surface than a single global table would have. Consumers learn about profiles, registries, and explicit profile selection.
- Initial profile coverage is limited. States without shipped profiles require either ICAO default (which may be wrong for them) or consumer-provided profiles.

**Neutral:**

- The design is consistent with the SDK's broader stance: explicit consumer choice over inferred behavior. The same pattern appears in other places (lookup tables expose recognition without gating, generation accepts unrecognized but well-formed codes).

---

## Alternatives Considered

**Single global table, per ICAO defaults.** Rejected because real states diverge from ICAO defaults. Producing MRZs that do not match what the issuing state actually issues is a serious correctness problem.

**Inferred profile selection based on issuing state.** Considered carefully. The pattern would be: the consumer provides the issuing state code, the SDK looks up the appropriate profile and applies it. Rejected because:
- The mapping from state to profile is not always one-to-one (a state may have multiple conventions for different document types or time periods).
- Inferred behavior is harder to debug. When the wrong characters appear in the MRZ, the consumer must trace why the SDK picked a particular profile.
- The reader-not-oracle stance is incompatible with inference: the SDK does not guess locale.
- Consumers who have not provided an issuing state would face an awkward fallback decision.

**Profile inheritance ("based on ICAO default with overrides").** Considered for the initial release. Deferred to a future enhancement (tracked in `open-questions.md`). The initial release uses standalone profiles; inheritance can be added without breaking existing consumers.

---

## Related Decisions

- ADR-004 — reader, not oracle. The "never inferred" rule is a direct expression of this stance.
- [ADR-014](0014-unicode-normalization-strategy.md) — Unicode normalization strategy. The implementation-strategy companion to this ADR: how the SDK handles Unicode equivalence (NFC vs NFD canonical forms) before profile lookup.

---

## Related Documents

- `transliteration.md` — describes the profile architecture in detail
- `mrz-generation.md` — describes when transliteration is invoked and what happens when no profile is provided
- `principles.md` — Principle 1 (Reader, not oracle) and Principle 5 (Transparency)
- `open-questions.md` — tracks the deferred profile inheritance feature
