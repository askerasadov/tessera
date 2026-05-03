# ADR-008: Date inference — hybrid raw + computed + inference method flag

**Status:** Accepted

---

## Context

Dates in the MRZ are encoded as YYMMDD — two-digit year, two-digit month, two-digit day. The two-digit year requires century inference: a passport expiring in `25` could mean 2025 or 1925.

ICAO Doc 9303 does not specify a century inference rule. The community consensus is that inference is technically impossible to do perfectly; implementations apply heuristics. Common heuristics include:

- **Expiry heuristic:** expiry dates are in the future or recent past; pick the century that places the date there
- **Sliding window for birth dates:** assume the most recent past century such that the implied age is plausible
- **Age-bound heuristic:** constrain to plausible age ranges (e.g., not older than 130 years)

A decision was needed about how the SDK exposes this to consumers.

---

## Decision

The SDK exposes dates with three components:

- **Raw values:** the two-digit year, two-digit month, and two-digit day exactly as they appeared in the MRZ
- **Computed values:** the four-digit year and full date as inferred by the SDK's heuristic
- **Inference method flag:** an enum value indicating which heuristic produced the computed year (`SLIDING_WINDOW_BIRTH`, `SLIDING_WINDOW_EXPIRY`, or `RAW_ONLY` when computation could not be performed)

This is the hybrid approach: convenience for consumers who want a usable year, transparency about the fact that inference was performed, and access to the raw value for consumers who need deterministic handling.

---

## Consequences

**Positive:**

- Honors Principle 1 (Reader, not oracle): the raw value is always available; the SDK does not pretend the computed year is fact.
- Honors Principle 5 (Transparency): nothing is hidden. The consumer can always see what the MRZ contained, what the SDK computed, and how.
- Honors Principle 4 (Honest about what we know): the inference method flag explicitly says "this is inferred via X heuristic," not just "this is the year."
- Pragmatically useful: most consumers want a year they can use; the computed year gives them one with appropriate metadata.
- Consumers needing deterministic handling (test environments, replay scenarios, audit pipelines) use the raw values and apply their own logic.

**Negative:**

- More complex API than either pure raw or pure computed would be. Consumers must understand that there are multiple representations and what they mean.
- Documentation must explain the inference methods clearly, including the time-dependence (the computed year can change as time passes if the heuristic depends on current time).
- The presence of a "computed" value may tempt consumers to use it without considering the inference flag, especially if their consumer onboarding does not emphasize the distinction.

**Neutral:**

- The design is consistent with how the SDK handles other potentially-ambiguous data (country codes exposed as raw + recognized flag, document type codes similarly). The pattern is familiar within the SDK.

---

## Alternatives Considered

**Pure raw (no computation).** Expose only the two-digit year; consumer applies any inference. Pros: most honest, no risk of consumers misusing computed values. Cons: every consumer must implement the same heuristic; the SDK's expertise on the heuristic is not shared.

**Pure computed (no raw).** Expose only the four-digit year inferred by the SDK's heuristic. Pros: simplest API. Cons: violates Principle 1 (the SDK is now claiming a year that is not in the document) and Principle 5 (raw value is hidden); creates reputation risk if the heuristic is wrong in cases where the consumer didn't realize inference was happening.

**Computed with a "confidence" or "warning" attached.** Considered as a middle ground: expose computed year, attach a warning when the inference is non-trivial. Rejected because every inferred year is non-trivial; the warning would always fire and become noise. The inference method flag is more informative because it tells the consumer *which* heuristic was used, not just "we guessed."

**Pass an explicit reference time to remove time-dependence.** Considered as a way to make the computed year fully deterministic regardless of when parsing happens. Partially adopted: the validator and other time-dependent operations may accept an explicit reference time. The data model itself uses current time by default but documents this clearly; consumers needing strict determinism can use the raw values.

---

## Related Decisions

- ADR-004 — reader, not oracle. The hybrid approach is a careful navigation of how to be helpful (computed year) while staying true to the reader stance (raw + flag).

---

## Related Documents

- `mrz-data-model.md` — defines the `MrzDate` type with all three components
- `mrz-parsing.md` — notes the time-dependent behavior of computed dates
- `mrz-validation.md` — uses dates in time-dependent semantic checks
- `principles.md` — Principles 1, 4, and 5
