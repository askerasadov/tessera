# ADR-006: No `isValid` boolean on results

**Status:** Accepted

---

## Context

Result types (`ParseResult`, `ValidationResult`, etc.) carry information about what was extracted, what validations passed, what failed, and what warnings exist. A common API convenience would be a single `isValid` boolean property that summarizes the result with one bit: true if everything passed, false if anything failed.

This convenience seems harmless. It saves consumers from inspecting validation lists when all they want is a yes/no answer. It is the kind of property that consumers expect from validation libraries.

A decision was needed about whether to provide it.

---

## Decision

The SDK does not provide an `isValid` boolean on any result type.

Consumers who want a single yes/no answer derive it from the underlying data according to their own definition of validity:
- Strict consumers: `errors.isEmpty() && validationFailures.isEmpty() && warnings.isEmpty()`
- Standard consumers: `errors.isEmpty() && validationFailures.isEmpty()`
- Lenient consumers: `errors.isEmpty()`

These are all defensible definitions. The SDK does not pre-decide which one is right.

---

## Consequences

**Positive:**

- Consumers cannot accidentally rely on a definition of validity that does not match their use case. They must explicitly state what they consider "valid" in their own code.
- The SDK has no opinion that needs defending or evolving. If we provided an `isValid` and later wanted to refine its meaning, that refinement would either break existing consumers or carry the wrong semantics.
- The result types remain honest about what they contain. A boolean summary suggests the SDK has made a judgment; the absence of one signals correctly that interpretation is the consumer's responsibility.
- Documentation can clearly describe what each list (errors, validation failures, warnings) means and let consumers map them to their needs.

**Negative:**

- Consumers who want a yes/no answer must write a small amount of code (typically one line) to derive it. This is a real friction point, however small.
- API documentation must explain why there is no `isValid` and what consumers should do instead. Consumers expecting it may be surprised.
- Library comparisons may show this SDK as "missing a basic feature" relative to oracle-style competitors.

**Neutral:**

- The decision is consistent with the reader-not-oracle stance (ADR-004). Providing `isValid` would be a small but real step toward oracle behavior.

---

## Alternatives Considered

**Provide `isValid` with a documented strict definition.** The SDK would document that `isValid` means "no errors, no validation failures, no warnings." Rejected because consumers who want a different definition either cannot use the property or use it incorrectly. The "strict" definition is the most defensible default but is too strict for many real use cases (e.g., a consumer who is fine with an expired-document warning).

**Provide multiple `isValid` variants** (`isStrictlyValid`, `isLooselyValid`, etc.). Rejected because it just shifts the problem: consumers must still pick which one fits their case, and the SDK has codified specific definitions that may not match any given consumer's policy. More API surface for the same problem.

**Provide a configurable `isValid` based on consumer-provided rules.** Rejected because at that point, the consumer is writing their own validity logic anyway. The SDK gains complexity without giving consumers anything they could not write themselves more cleanly.

---

## Related Decisions

- ADR-004 — reader, not oracle. The absence of `isValid` is a direct consequence of refusing to make trust decisions on the consumer's behalf.
- ADR-005 — no verification hooks. Both decisions push trust judgments to the consumer.

---

## Related Documents

- `mrz-data-model.md` — defines the result types and explicitly notes the absence of `isValid` with the same reasoning
- `mrz-validation.md` — restates the no-`isValid` commitment in the context of the validation feature
- `principles.md` — Principle 1 (Reader, not oracle) and Principle 5 (Transparency — no summarization that hides detail)
