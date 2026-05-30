# ADR-014: Unicode normalization via platform-native normalizers, exposed through expect/actual

**Status:** Accepted

---

## Context

[ADR-009](0009-transliteration-profiles.md) establishes the SDK's transliteration architecture: per-state profiles that convert non-MRZ-alphabet characters into the MRZ's restricted 37-character set (A–Z, 0–9, filler `<`). The consumer specifies which profile to use; the profile defines the mapping rules. None of this is in scope for re-litigation here.

The implementation-strategy question that ADR-009 left open is **how the SDK handles Unicode equivalence**. A character with a diacritic (e.g., `é`) can be encoded in two canonically-equivalent forms:

- **NFC** (canonical composition) — `U+00E9`, a single precomposed code point.
- **NFD** (canonical decomposition) — `U+0065 U+0301`, the base letter plus a combining accent.

These two byte sequences are *defined as equivalent* by the Unicode standard. To a human reader they are identical. To naïve lookup-based code they are distinct keys. If a transliteration profile's lookup table contains an entry for the precomposed form but the consumer's input arrives in the decomposed form (or vice versa), the lookup misses and the character is treated as unsupported.

The failure mode is silent: the consumer gets `MrzGenerationUnsupportedCharacters` for input that "looks" valid, or — depending on profile fallback policy — a different transliterated output than they expected. Both outcomes are surprising in a way that is hard to diagnose after the fact.

Normalization forms vary by source platform in practice. macOS uses NFD on the filesystem (HFS+'s legacy decision), Windows and most Linux tools default to NFC, Web browsers vary by input method, and mobile keyboards can produce either form. Real consumer input will arrive in mixed normalization forms.

This decision was deferred until the pre-release tech-stack review (per the "Pre-Release Tech-Stack Review" rule in [`CLAUDE.md`](../../CLAUDE.md), landed in PR [#31](https://github.com/lightine-io/tessera/pull/31)) because committing to a Unicode strategy locks foundational infrastructure under [ADR-007](0007-strict-backward-compat-from-0x.md) once `0.1.0` ships. The review was conducted in the 2026-05-17 session before any transliteration code was written.

---

## Decision

The SDK normalizes consumer input to **NFC (Normalization Form Canonical Composition)** before profile lookup, using each platform's native Unicode normalizer exposed through a Kotlin Multiplatform `expect`/`actual` declaration.

Concretely:

- An internal `expect fun normalizeForTransliteration(input: String): String` lives in `commonMain` of `mrz-core`. Profile lookup operates on the post-normalization string.
- Each enabled target supplies an `actual` implementation that delegates to the platform's standard library:
  - **JVM:** `java.text.Normalizer.normalize(input, Normalizer.Form.NFC)`
  - **Android:** identical to JVM (Android ships `java.text.Normalizer`)
  - **iOS (Konan):** `(input as NSString).precomposedStringWithCanonicalMapping`
  - **JS / Wasm:** `input.normalize("NFC")`
- For `0.1.0` only the JVM `actual` is implemented, because JVM is the only target enabled in this release (per [`docs/scope.md`](../scope.md) Supported Platforms). Other actuals are written when their targets activate (Android in `0.2.0` alongside camera reading; iOS when Xcode is available on a development machine; JS/Wasm on demand). **Status update (2026-05-30):** the **Android** and **iOS** `actual`s both landed in the `0.2.0` build-foundation slices — Android mirrors the JVM `java.text.Normalizer`; iOS uses the `NSString.precomposedStringWithCanonicalMapping` form shown above, verified on the iOS simulator across the full `mrz-core` test suite. JS/Wasm remain on demand.
- The choice of **NFC** (composition) rather than NFD (decomposition) follows the W3C Character Model recommendation and the IETF guidance for text on the wire. NFC is also the more common form in stored text data, minimizing the work the normalizer has to do on typical input.
- Normalization is **locale-independent**. The SDK does not pass a locale to the normalizer; locale-specific transformations (e.g., Turkish dotted/dotless `i`) are the responsibility of country-specific profiles, not the normalization step.

Per [Principle 5](../principles.md) (Transparency over Magic), the post-normalization, pre-transliteration form is exposed to consumers. The concrete surface is decided when transliteration ships — likely a field on the generation result metadata carrying the normalized input alongside the transliterated output — but the requirement to expose it is locked here so it cannot be skipped in implementation.

---

## Consequences

**Positive:**

- Platform-native normalizers are the most rigorously tested Unicode code in their respective ecosystems. The SDK does not compete with Apple, Google, Oracle, or the V8/SpiderMonkey teams by writing or vendoring its own. ([Principle 2](../principles.md) — logical defensibility.)
- The architectural boundary is clean: input enters the normalizer, normalized output enters the profile, transliterated output exits. If normalization strategy ever needs to change (e.g., a new Unicode form), only the `actual` implementations change; profiles and the public API are unaffected. ([Principle 3](../principles.md) — modular architecture.)
- Profile tables stay small (~80–100 entries) instead of carrying every variant form of every character. New profiles are easier to add and audit.
- The decision is consistent with [Principle 8](../principles.md) (native fit over cross-platform purity): share what is genuinely platform-independent (the transliteration logic and profile registry), use platform primitives where the platform shines (Unicode normalization).
- Adding a new target later is mechanical: write one `actual` implementation, no changes to common code, no API surface change. ([Principle 9](../principles.md) — forward-compatible API.)
- Unmapped characters continue to surface as the existing typed error (`MrzGenerationUnsupportedCharacters`); no silent failure mode. ([Principle 7](../principles.md) — fail loudly.)

**Negative:**

- One `expect` declaration plus one `actual` per target. As of `0.1.0` only the JVM actual existed; the Android and iOS actuals landed in `0.2.0` (see the Decision status update above). The scaffolding is a handful of lines per target. Not free, but minimal.
- The iOS `actual` could not be written until Xcode was available on a development machine — not a `0.1.0` blocker because iOS was not a `0.1.0` target. **Resolved in `0.2.0`:** Xcode 26.5 is present and the iOS `actual` is implemented; the [`open-questions.md`](../open-questions.md) entry "iOS target configuration on core modules" is marked executed.
- Consumers cannot trivially opt out of normalization. The design choice is intentional — bypassing normalization is the silent-failure mode this ADR is designed to eliminate — but consumers with unusual requirements (e.g., binary-exact pass-through for testing) will need to construct their input in NFC form before passing it to the generator.

**Neutral:**

- NFC is canonical equivalence, not interpretation. The Unicode standard defines NFC and NFD as representing the same character; converting between them does not change *which character* is being represented. This is mathematical equivalence, not the SDK guessing consumer intent. ([Principle 1](../principles.md) — reader, not oracle, holds.)
- Locale-independent normalization is the right default for an MRZ SDK whose consumers span every issuing state. Locale-specific transformations belong in profiles where they are documented and selected explicitly.

---

## Alternatives Considered

**Pure-Kotlin Unicode normalization library (Strategy B).** Bring in a `commonMain` Unicode library (an ICU4K-style port or similar). Rejected because:

1. The SDK would inherit a dependency on a library whose maintenance horizon is uncertain. Pure-Kotlin Unicode normalization libraries exist but are thin compared to platform stdlib normalizers. ([Principle 2](../principles.md) — fewer assumptions.)
2. Binary size grows for every consumer, even those who never use transliteration.
3. The "free" benefit (no `expect`/`actual` boilerplate) is small in absolute terms — ten lines of scaffolding plus one platform delegation per target.

**Lookup-table-only with no normalization (Strategy C).** Skip the normalization step entirely; require every profile table to enumerate both precomposed and decomposed variants of every character. Rejected because:

1. The failure mode for un-enumerated variants is silent. A consumer who pastes input in an unusual normalization form (common when text crosses platforms) gets either a `MrzGenerationUnsupportedCharacters` error or — worse — a profile fallback the consumer didn't anticipate, with no signal that normalization-variance was the cause. ([Principle 4](../principles.md) — honest about what we know; [Principle 7](../principles.md) — fail loudly, fail informatively.)
2. Profile tables grow 2–3× to cover variants. Auditing a profile for correctness becomes much harder; adding new profiles becomes correspondingly more work.
3. ADR-007 strict-backcompat means the strategy chosen at `0.1.0` is locked. If lookup-table-only ships and the silent-failure mode proves painful in practice, adding normalization later is a breaking change to the transliteration semantics. ([Principle 9](../principles.md) — design APIs to evolve.)

**Defer the decision (Strategy D).** Ship `0.1.0` transliteration without a Unicode-handling strategy; document the limitation and revisit in a later release. Rejected because:

1. Shipping a known silent-failure mode contradicts the project's stance on honest behavior. A `0.1.0` consumer using transliteration in good faith would encounter the variant-mismatch bug without any way to know it existed.
2. The decision moves into the strict-backcompat zone the moment `0.1.0` tags. Deferring does not avoid the lock-in; it just makes the eventual decision happen under pressure.
3. The work to do it right now (one expect declaration, one JVM actual, ~10 lines) is smaller than the documentation burden of explaining what the gap is and why consumers should be cautious.

---

## Related Decisions

- **[ADR-004 — Reader, not oracle.](0004-reader-not-oracle.md)** Normalization is canonical equivalence per the Unicode standard, not the SDK interpreting consumer intent. The principle holds.
- **[ADR-007 — Strict backward compatibility from 0.x.](0007-strict-backward-compat-from-0x.md)** The reason this decision needs a permanent record: once `0.1.0` ships, changing the Unicode normalization strategy is a breaking change to the transliteration contract.
- **[ADR-009 — Per-state transliteration profiles, never inferred.](0009-transliteration-profiles.md)** The parent architectural decision; this ADR is its implementation-strategy companion. ADR-009's `Related Decisions` section is updated to cross-reference this ADR.

---

## Related Documents

- [`principles.md`](../principles.md) — Principles 1, 2, 3, 4, 5, 7, 8, 9 referenced above.
- [`docs/features/transliteration.md`](../features/transliteration.md) — feature document; updated to cross-reference this ADR.
- [`docs/scope.md`](../scope.md) — Supported Platforms (the per-release target-activation model that defines when each `actual` lands).
- [`CLAUDE.md`](../../CLAUDE.md) — the Pre-Release Tech-Stack Review rule under which this decision was made.
