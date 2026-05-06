# ADR-013: Recognition failures are warnings, not validation errors

**Status:** Accepted

---

## Context

Several validators in the SDK check whether a code observed in an MRZ field is recognized in a lookup table the SDK ships. The two clearest examples are:

- **Document type code recognition** — the `DocumentType` value class wraps a one or two-character code and consults `DocumentTypeCodeTable` per ADR-012. The validator surfaces the recognition state through a typed validator output (`MrzUnknownDocumentTypeCode`).
- **Country code recognition** — the (planned) `CountryCode` value class will follow the same pattern with `CountryCodeTable`. The validator output is `MrzUnknownCountryCode`.

`ValidationResult` carries two parallel lists with different semantic weight:

- `validationFailures: List<MrzValidationError>` — items that mean *"the document does not conform to the spec."*
- `warnings: List<MrzWarning>` — items that mean *"the data is valid, but anomalous; the consumer may or may not care."*

These two outputs are used by the same kind of consumer to decide whether to accept or reject a document. The categorical placement of any given check (warning vs. failure) is therefore part of the public contract — and per ADR-007 (strict backward compatibility from 0.x), once 0.1.0 ships the placement is frozen within MAJOR. Walking back from "failure" to "warning" later is a breaking change for any consumer reading `validationFailures`. Walking back from "warning" to "failure" is breaking for any consumer reading `warnings.isEmpty()` as a strictness gate.

Two existing prose passages in the project's documentation already commit to "warning":

- `docs/features/lookup-tables.md` — *"The validator surfaces unrecognized codes as warnings (`MrzUnknownCountryCode`, `MrzUnknownDocumentTypeCode`), not errors."*
- `docs/features/mrz-validation.md` ("Recognition vs Conformance") — *"The validator distinguishes between 'well-formed but unrecognized' (a warning) and 'structurally invalid' (a failure)."*

A third passage in `docs/features/mrz-error-taxonomy.md` listed both unknown-code variants under "Validation Failures (data extracted but does not conform)" — inconsistent with the other two, and inconsistent with the principles below. That listing is treated by this ADR as a placement error and is corrected together with this decision.

`docs/open-questions.md` carried two related entries that this ADR addresses: "Document type code recognition validation (`MrzUnknownDocumentTypeCode`)" framed the failure-vs-warning question as undecided; "Document type code table completeness" tracked the deliberate incompleteness of the SDK's starter set (6 codes today vs. the full ICAO list).

A decision was needed because the first concrete validator wiring (`MrzUnknownDocumentTypeCode` in `MrzValidator.validateTD3`) has to land on one side of the line, and that placement should rest on principle rather than precedent — once it ships, every analogous future check (country code, additional recognition tables) is bound by the same line.

---

## Decision

A validator output that signals *"the observed code is not in the SDK's recognized lookup tables"* is a warning, not a validation failure.

Concretely:

- `MrzUnknownDocumentTypeCode` extends `MrzWarning` and is emitted into `ValidationResult.warnings`.
- `MrzUnknownCountryCode`, when implemented, will extend `MrzWarning` and be emitted the same way.
- Any future recognition-table check that reduces to *"this code is not in our table"* follows the same rule.

The rule does not extend to checks that reduce to *"this value is impossible per the spec"* (e.g., `MrzCheckDigitMismatch`, `MrzInvalidSexValue`, `MrzDateNotInCalendar`). Those remain validation failures because the SDK can verify their wrongness independently of any table the SDK happens to ship.

---

## Consequences

**Positive:**

- The validator's output is honest about what the SDK actually knows. An unknown code surfaces *"the SDK does not recognize this code"* — not *"this code is not in ICAO Doc 9303."* The latter is a stronger claim than the SDK can support, given the table is deliberately incomplete and ICAO updates the spec periodically.
- The decision is consistent with Principle 1 (Reader, not oracle). The SDK reports the observation; the consumer makes the trust decision.
- The decision is consistent with Principle 4 (Honest about what we know). Treating the table's incompleteness as if it were authoritative would pretend certainty the SDK does not have.
- A strict consumer who wants unknown codes to be disqualifying gets that with `result.validationFailures.isEmpty() && result.warnings.isEmpty()`. A lenient consumer who wants only spec-violations to disqualify gets that with `result.validationFailures.isEmpty()`. Both modes are one expression each, no filtering.
- The decision generalizes. Every recognition-table-derived check the SDK adds in the future lands on the same side of the line for the same reason, without re-litigating.

**Negative:**

- Strict consumers who expect a single `validationFailures` list to contain every disqualifying signal must read `warnings` too. This is a mild ergonomic cost compared to the alternative of overclaiming.
- Documentation drift becomes possible if a contributor adds a new recognition-bearing check and reaches for `MrzValidationError` by reflex. The taxonomy doc's "Warnings" section is the canonical placement for these types and should be referenced when adding new ones.
- The ADR-007 strict-backcompat clock makes this hard to reverse from 0.1.0 onward. If a future ICAO update or a future SDK feature gives the SDK genuine authority to declare an unknown code non-conformant, the change to "failure" is a breaking change. This is accepted; the principle-based reasoning is durable enough that the question is unlikely to flip.

**Neutral:**

- The SDK already exposes the recognition signal directly on the model (`DocumentType.isRecognized: Boolean`, accessible without going through the validator). The warning is the validation-result-formatted version of the same signal — no information is added or hidden by either choice.

---

## Alternatives Considered

**Treat unrecognized codes as validation failures** (the alternative this ADR rejects). Rejected because:

1. The SDK's recognition table is deliberately incomplete and tracked as such. Emitting a failure for a code missing from a partial table claims authority the SDK does not have. Even if the table were complete-as-of-today, ICAO updates the lists periodically; a code may be valid but newer than the SDK's data.
2. The placement disagrees with two existing prose commitments in the docs and with Principle 1 (Reader, not oracle).
3. The strict consumer's ergonomic cost (having to filter on warnings too) is small. The honest-about-knowledge cost of overclaiming is larger.

**Treat them as failures, but document the failure as "weak"** (some kind of severity field). Rejected because severity-on-failure is a new concept that doesn't exist elsewhere in the result type. Two parallel lists with clear semantic weight (failure = non-conformance, warning = anomaly) is simpler and already established. Adding a severity dimension would obscure rather than clarify.

**Suppress the typed output entirely; only expose `DocumentType.isRecognized` on the model.** Rejected because consumers consuming `ValidationResult` should not have to know to also reach into the document model to find the recognition state. The two surfaces are complementary; both should carry the signal.

---

## Related Decisions

- **ADR-004 — Reader, not oracle.** The principle this ADR most directly applies. Recognition state is data the SDK observes; deciding what unrecognized means for trust is a consumer responsibility.
- **ADR-007 — Strict backward compatibility from 0.x.** The reason this categorical placement decision needs a permanent record: once 0.1.0 ships, moving an output between `validationFailures` and `warnings` is a breaking change.
- **ADR-012 — Recognition-bearing value classes live with their lookup tables.** Establishes where the recognition signal originates; this ADR establishes how the validator surfaces it.

---

## Related Documents

- `principles.md` — Principle 1 (Reader, not oracle) and Principle 4 (Honest about what we know), the principles this decision applies.
- `docs/features/lookup-tables.md` — already commits to "warning, not error" for unrecognized codes; this ADR formalizes the reasoning.
- `docs/features/mrz-validation.md` — describes the layered validation model and the recognition-vs-conformance distinction; this ADR aligns with the existing prose.
- `docs/features/mrz-error-taxonomy.md` — the catalog of validator outputs. Updated together with this ADR to move `MrzUnknownCountryCode` and `MrzUnknownDocumentTypeCode` from "Validation Failures" to "Warnings."
- `docs/open-questions.md` — the entry "Document type code recognition validation (`MrzUnknownDocumentTypeCode`)" is resolved by this ADR.
