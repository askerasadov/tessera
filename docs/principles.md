# Project Principles

This document captures the foundational principles that guide every design and implementation decision in this project. They are not aspirations — they are operating rules. When a decision is ambiguous, we resolve it by checking which option is more consistent with these principles.

The principles are numbered for reference but not ranked. They are intended to work together. When two principles appear to conflict, the resolution is usually that we have not understood one of them precisely enough — both should hold simultaneously when applied correctly.

This document is living. Principles can be revised, added, or removed, but only through deliberate discussion and with reasoning recorded. Drift by accident is not allowed.

The principles are written to be target-agnostic. The project's first concrete features are oriented toward mobile platforms, but the SDK is not fundamentally mobile-only. Most of its logic — document parsing, validation, protocol handling, format conversions — is platform-independent and useful in any context where document data needs to be read or generated, including backend services, desktop applications, and web environments. Specific principles may reference platforms by name when illustrating a point, but the principles themselves apply to every target the project may serve, present or future.

---

## Principle 1 — Reader, Not Oracle

**The SDK extracts data verbatim from documents. It does not modify, correct, or improve what it reads. Trust decisions belong to consumers, not to the SDK.**

When a document contains data — whether on a printed page, in a Machine Readable Zone, or on a chip — the SDK's job is to read that data faithfully. If the data is misspelled, the SDK reproduces the misspelling. If a field is unexpected, the SDK exposes it as-is. If a value seems wrong, the SDK does not silently correct it.

This principle exists because the SDK cannot know what the consumer will do with the data, what business rules apply, or what legal frameworks govern the use case. The consumer has that context; the SDK does not.

The consequence: the SDK never refuses to return data because something looks wrong. It returns the data along with a complete report describing what was extracted, what validations passed, and what anomalies were observed. Refusal is a consumer decision.

This applies to all features. If we ever add liveness detection, selfie capture, or other features, they obey the same rule: read the input as it is, report what we observed, let the consumer decide.

---

## Principle 2 — Logical Defensibility Over Convenience

**When choosing between options, prefer the one that requires fewer assumptions about things we cannot verify. Choose options that survive being wrong about predictions.**

Many software decisions are bets on the future: which technologies will remain stable, which features users will demand, which integrations will become important. These bets are often wrong. A decision that depends on a correct prediction is fragile; a decision that holds up regardless of how the prediction resolves is durable.

This principle pushes us toward simpler architectures, fewer dependencies, broader contracts, and more conservative scope. It pushes us away from speculative features ("we might need this someday"), opinionated abstractions ("everyone will want it this way"), and tightly coupled designs ("this won't ever change").

When two options are roughly equal in cost and capability, the one that assumes less wins.

---

## Principle 3 — Modular Architecture, Not Monolith

**Every component is replaceable. Every dependency is bounded. We can swap UI frameworks, parsers, transports, and protocols, as long as the contracts are honored.**

Modularity is not a slogan. It is a structural commitment: the project is composed of distinct units that depend on each other through clear, narrow interfaces. Internal complexity within a unit is acceptable. Cross-unit complexity is not.

This applies at three levels:

- **Internal modules** — the SDK is composed of multiple modules with explicit dependencies; circular dependencies are forbidden.
- **Chosen technologies** — we use specific tools (Kotlin, native UI frameworks, etc.) but our designs do not assume those tools forever; replacing one should be mechanical, not architectural.
- **Consumer-facing APIs** — public surfaces are stable contracts; internal implementations can change freely.

The benefit is real: when something changes (a library, a protocol, a platform), only the affected module changes. The rest of the project keeps working.

The cost is also real: modular designs require more upfront thought, clearer interfaces, and discipline to maintain. We accept this cost intentionally.

---

## Principle 4 — Honest About What We Know

**Do not pretend certainty we do not have. Decisions are marked as current intent when that is what they are. Document assumptions and reasoning. Say "we don't know yet" when we don't.**

This is partly a writing rule (use precise language) and partly a design rule (do not over-commit). The goal is that anyone reading our documentation, code, or comments can tell the difference between:

- A settled fact ("the MRZ is 88 characters in TD3 format")
- A current decision ("we use Kotlin Multiplatform for shared logic")
- An open question ("we have not yet decided whether to support format X")
- A speculation ("this might be useful in the future")

Mixing these up creates confusion. Treating speculation as fact creates bugs. Treating current decisions as eternal creates rigidity.

When we make a decision based on incomplete information, we say so. When we change our mind later, that is normal — but the original reasoning stays in the historical record so the change is understandable, not arbitrary.

---

## Principle 5 — Transparency Over Magic

**The SDK exposes everything it extracts. Nothing is hidden, nothing is computed and thrown away. If we extract data internally, we expose it externally.**

Consumers should never need to fork the SDK to get at data we already have. If our internal logic computes a value, that value is part of the public output. If a verification step produces evidence, that evidence is exposed alongside the result.

This principle is closely related to Principle 1 (Reader, Not Oracle). Together they define a stance: the SDK provides primitives; the consumer composes meaning. The SDK does not act as a black box that emits opinions; it acts as a window into the document, and the consumer reads through it.

The practical consequences:

- Output structures are richer than minimum-needed; they include raw data, parsed fields, validation results, and metadata.
- We do not collapse multiple internal signals into a single boolean output; consumers see the signals.
- We provide ways to access intermediate results (raw MRZ string, raw chip data groups, raw signature material) for consumers who need them.

If something is in the document, it is in the SDK's output.

---

## Principle 6 — Defense in Depth, Not Security Theater

**Security comes from cryptographic protocol and proper memory hygiene, not from UI ownership or obfuscation. We honestly tell consumers what the SDK can and cannot defend against.**

Client-side SDKs cannot prevent every attack. A malicious or compromised consumer application can hook our methods, replace our UI, or bypass our checks. A determined attacker on a compromised host (rooted device, modified runtime, instrumented process) can defeat any in-process protection. Pretending otherwise is dishonest.

What we can do:

- Use platform cryptographic primitives correctly (hardware-backed keys, secure elements where available).
- Maintain strict memory hygiene for sensitive data (clear after use, avoid leak-prone types).
- Issue cryptographic challenges that require hardware-backed responses, so trivial replay attacks fail.
- Detect signals like rooting, debugging, and emulation, and report them to the consumer.

What we will not do:

- Bundle UI for sensitive operations and claim that the UI provides security.
- Obfuscate our code and claim the obfuscation provides security.
- Make claims about security guarantees we cannot honestly support.

We document what we defend against, what we don't, and where the consumer's responsibility begins. Honest descriptions of limitations are part of being trustworthy.

---

## Principle 7 — Fail Loudly, Fail Informatively

**Errors are typed, specific, and exhaustive. We never swallow exceptions or return null for ambiguous failures. Consumers always know what went wrong and why.**

Silent failures are the worst kind. A function that returns null when something failed forces the consumer to guess what happened. A function that throws a generic exception with no context forces the consumer to handle every possibility identically. Both make integration harder, harder to debug, and easier to misuse.

Our approach:

- Errors are sealed types (or equivalent), enumerating every possible failure mode.
- Each error carries enough context to understand the failure (which field, which step, which protocol).
- We distinguish between three categories: errors (operations that did not complete), validation failures (data extracted but does not conform to spec), and warnings (data is valid but anomalous).
- We do not collapse multiple failure modes into a generic "something went wrong."

This makes the SDK harder to write internally — every error needs naming and structure. It makes the SDK significantly easier to integrate, debug, and trust.

---

## Principle 8 — Native Fit Over Cross-Platform Purity

**Where it matters — UI, security primitives, idiomatic API surface — we choose native fit. Where it does not — parsing, validation, protocol logic — we share via cross-platform tooling. Each tool is used where it shines.**

Cross-platform code sharing is valuable for logic that is genuinely platform-independent. Document parsing, check digit algorithms, protocol implementations, country code lookup tables — these are the same on every operating system and benefit from a single source of truth.

UI, security primitives, and platform-idiomatic APIs are different. On platforms where users have established expectations, we honor those expectations. A scanner UI on iOS should feel like an iOS scanner UI; on Android, like an Android scanner UI; on a desktop platform, like a desktop application. Hardware-backed cryptography uses different APIs on each platform. Consumer developers expect APIs that feel idiomatic to their language and ecosystem — Swift-native on iOS, Kotlin-native on Android, language-appropriate elsewhere.

We do not impose cross-platform purity where the cost is degraded native experience. We do not duplicate logic where sharing is straightforward. The boundary between shared and platform-specific is drawn deliberately, not dogmatically. The set of supported platforms can grow over time without revisiting this principle — the rule applies wherever the SDK runs.

---

## Principle 9 — Forward-Compatible API, Opinionated Implementation

**Public APIs are designed to evolve. Adding capabilities is non-breaking; removing is rare and deprecated first. Internal implementations can change freely as long as contracts hold.**

The promise to consumers is stability of the surfaces they integrate against. The flexibility we keep for ourselves is the freedom to change how those surfaces are implemented.

Practical implications:

- New fields can be added to result types without breaking existing consumers.
- New methods can be added without breaking existing consumers.
- New optional parameters use safe defaults so existing call sites still compile.
- Removals follow a deprecation cycle: marked deprecated in version N, removed in version N+2 at earliest.
- Internal refactors do not require coordinated consumer updates.

This requires discipline at API design time: we think carefully before exposing something publicly, because we will live with it. It also requires discipline at maintenance time: we do not break consumers casually.

---

## Principle 10 — Privacy by Default

**No storage. No phone-home. No personally identifiable information in logs. Telemetry is opt-in via a consumer-provided implementation. Sensitive data lives only as long as needed.**

The SDK handles identity documents. The data inside those documents is, by any reasonable definition, sensitive. Our default behavior reflects that.

Specifically:

- The SDK does not persist any document data, anywhere, by default.
- The SDK does not contain hardcoded URLs to external services. It does not phone home for telemetry, analytics, or licensing checks.
- Sensitive data in memory (extracted fields, cryptographic keys, raw chip data) is cleared promptly after use using techniques appropriate to the platform.
- Logging is configurable by the consumer and never includes raw document data. Diagnostic information is structured to convey context without leaking content.
- Telemetry, when needed, is plugged in by the consumer through a well-defined interface. The SDK has no built-in telemetry destination.

Consumers can choose to log, store, or transmit data themselves — that is their decision and responsibility. The SDK does not make that decision for them.

---

## Principle 11 — Internal Packages First, Standalone Modules When Justified

**New features start as internal packages with clean public API boundaries inside existing modules. They are promoted to standalone modules only when independent reuse, evolution, testing, ownership, shipping, or optional inclusion clearly applies.**

This is the operational rule for applying Principle 3 (Modular Architecture). Modularity is a goal, but creating modules eagerly is a cost without a benefit when no module-level concern is at play.

A new feature begins as a clearly bounded package within an existing module. Its public API is defined, its internal structure is its own. If, over time, one of the following becomes true:

- The feature is genuinely useful without the parent module (independent reuse).
- The feature evolves at a different pace than its parent (independent evolution).
- The feature requires its own testing context (independent testing).
- The feature is owned or maintained by different people (independent ownership).
- The feature ships on a different schedule (independent shipping).
- The feature is optional and consumers should be able to exclude it (optional inclusion).

… then we promote it to a standalone module. Because the package boundary already exists with a defined API, the promotion is mechanical, not architectural.

This avoids both extremes: the monolith that grows without internal structure, and the over-modularized project where every concept is a separate artifact regardless of need.

---

## How to Use These Principles

When facing a decision, ask which option is more consistent with the principles. Often, this gives a clear answer.

When two options both seem consistent, the principles are not the deciding factor — pick by other criteria (cost, time, preference) and document the choice.

When two principles seem to conflict in a specific case, that is a signal to look harder: usually one of them is being interpreted too broadly. Both should hold simultaneously when applied carefully.

When a principle gets in the way of something that genuinely needs to happen, that is a signal to reconsider the principle, not to violate it silently. Revise the principle through deliberate discussion, with the reasoning recorded.

These principles are not the project's complete philosophy. They are the parts we have made explicit so far. Other commitments will emerge as we build, and may eventually be added here.
