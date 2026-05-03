# ADR-004: Reader, not oracle as foundational stance

**Status:** Accepted

---

## Context

An identity document SDK could position itself in one of two fundamentally different ways:

1. As an **oracle** — the SDK extracts data, applies rules, and returns judgments ("this document is valid", "this person is who they claim to be", "this MRZ should be rejected because of X").
2. As a **reader** — the SDK extracts data faithfully and reports observations, leaving all trust decisions to the consumer.

These are not stylistic differences — they shape the entire API, the documentation, the relationship with consumers, and the SDK's responsibilities. A decision was needed before any feature work began, because every subsequent design choice depends on which stance the SDK takes.

---

## Decision

The SDK is a reader, not an oracle. It extracts data verbatim from documents, never modifies or corrects what it reads, and reports observations along with the data. Trust decisions belong to consumers, not to the SDK.

This is captured as Principle 1 in `principles.md` and is the most foundational of all the project's commitments. Every other principle and design decision either implements this stance or is constrained by it.

---

## Consequences

**Positive:**

- The SDK has a clear and defensible scope: extract data, report what was observed. Consumers cannot legitimately blame the SDK for wrong trust decisions, because the SDK does not make them.
- The SDK can be used in any consumer context — high-stakes border control, low-stakes form autofill, regulatory compliance flows, internal tooling — without the SDK needing to anticipate the consumer's policies.
- API design is consistent: every operation returns data plus metadata, never a verdict. Consumers learn one pattern.
- The SDK's testability improves because behavior is predictable and bounded by what's in the input, not by elaborate rule systems.

**Negative:**

- Consumers must do more work than they would with an oracle SDK. They have to interpret validation results, decide what failures mean for their use case, and apply their own trust policies.
- Onboarding documentation must explain the stance explicitly, because consumers familiar with oracle-style SDKs may expect verdicts.
- Some convenience features that would be natural in an oracle SDK (an `isValid` boolean, automatic recovery from common errors, "smart" fallbacks) are deliberately not provided.
- Marketing the SDK is harder. "We extract data and report what we saw" is less catchy than "We verify identity documents."

**Neutral:**

- The SDK can be extended later with optional convenience layers (helpers that apply common policies for common use cases) without changing the core stance, because such layers can be additive — built on top of the reader, not replacing it.

---

## Alternatives Considered

**Oracle stance.** The SDK applies rules and returns judgments. Rejected because:
- The SDK cannot know the consumer's threat model, regulatory environment, or business context. Whatever rules the SDK applies will be wrong for some consumers.
- An oracle SDK accepts liability for its judgments. A reader SDK does not.
- Oracle behavior is harder to evolve: changing what counts as "valid" affects all consumers simultaneously, even those whose use cases were happy with the old behavior.

**Hybrid stance with a default policy.** The SDK acts as a reader by default but ships a default policy that produces verdicts. Rejected because the default policy creates the same problems as a full oracle (consumers may use it without realizing it doesn't fit their case), and consumers who want a policy can implement one over the reader interface trivially.

**Reader stance with required validation.** The SDK extracts data but refuses to return data that fails validation. Rejected because validation criteria are themselves consumer-specific. Some consumers want check-digit failures to disqualify; others want to record the failure but accept the data. Refusing to return data takes away the consumer's option.

---

## Related Decisions

- ADR-005 — no verification hooks in initial release; a direct consequence of the reader stance
- ADR-006 — no `isValid` boolean; a direct consequence of the reader stance
- ADR-008 — date inference exposes raw + computed + flag, never just computed; honors the reader stance even where convenience would tempt otherwise
- ADR-009 — transliteration never inferred; the SDK does not guess locale, the consumer specifies

---

## Related Documents

- `principles.md` — Principle 1, defining this stance
- `reading-risks.md` — the document that explains, per reading method, what the SDK does and does not establish; a direct expression of the reader stance
- `mrz-data-model.md` — the data model design, which exposes raw fields alongside any computed interpretations
