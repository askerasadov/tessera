# ADR-018: Platform minimums and the managed-raise policy

**Status:** Accepted

---

## Context

`0.2.0` is the first Tessera release to ship for Android and iOS (see [ADR-017](0017-mobile-targets-and-build-stack.md)), so it is the moment their **minimum supported versions** — Android `minSdk` and the iOS deployment target — are *first set*.

This matters because [`versioning.md`](../versioning.md) classifies *raising* a minimum supported platform version as a **breaking change**, while *lowering* one is non-breaking. Under [ADR-007](0007-strict-backward-compat-from-0x.md) (strict backward compatibility from `0.x`), a breaking change is a major-version event. So the values chosen here are a real commitment and should be chosen for the whole `0.x` arc (camera now, UI at `0.5.0`, NFC at `0.6.0`), not just `0.2.0` — and we need a stated policy for the cases where a raise is nonetheless forced.

There is also a documentation tension to resolve: `scope.md` says a minimum "may be raised per release," while `versioning.md` says raising one is breaking. Both cannot stand unqualified.

## Decision

**Set the platform minimums low and vendor-aligned, and govern future changes with a managed-raise policy.**

- **Android — `minSdk 23`** (Android 6.0).
- **iOS — deployment target 18.**
- **Rationale (library, not app).** Tessera is a *library*; a low `minSdk` maximises the set of consumer apps that can adopt it, and the consumer's own app sets the real floor. Android 23 matches the AndroidX libraries' own default minimum (chosen by Google to reach ~99% of devices) and is far below what our camera dependencies (CameraX, ML Kit) require. iOS 18 follows Apple's new-product norm (~88% device reach) and leaves headroom above Kotlin/Native's own minimum iOS deployment target, which rises across Kotlin releases (Kotlin 2.3 = iOS 14). The minimums are set for the whole `0.x` arc with the future UI (`0.5.0`, SwiftUI) and NFC (`0.6.0`) in mind, since raising later is breaking.
- **Managed-raise policy.** Raising a minimum is a **documented breaking release** — release-noted, with prior artifacts left published so existing consumers are not retroactively broken (the approach AndroidX takes). It is done **only when forced**: a platform/vendor end-of-life, an unpatched security issue at the old floor, or a toolchain floor (notably Kotlin/Native's iOS minimum) climbing past ours. It is *neither* a casual per-release bump *nor* forbidden until 1.0. **Lowering** a minimum (broadening support) stays non-breaking.
- **Documentation reconciliation.** `scope.md`'s "may be raised per release" is read through this policy: a raise is permitted but is a documented breaking release made only when forced — consistent with `versioning.md`. Both documents are updated to state this.

## Consequences

**Positive:**
- Minimums are set deliberately, once, for the whole `0.x` line — avoiding a surprise forced breaking raise mid-stream.
- A low Android floor keeps Tessera maximally adoptable as a library.
- The policy gives a clear, non-arbitrary rule for the rare forced raise, and resolves the `scope.md` ↔ `versioning.md` tension.

**Negative:**
- iOS 18 excludes the small share of devices on older iOS. Acceptable given the library-adoptability framing and that lowering later is non-breaking if a consumer needs it.
- A forced raise (e.g. a future Kotlin/Native iOS-floor bump) is still a breaking, major-version event — the policy manages the cost, it does not eliminate it.

**Neutral:**
- `minSdk` / deployment-target are runtime *floors*; the *build* targets the latest stable SDKs ([ADR-017](0017-mobile-targets-and-build-stack.md)). The two move independently.

## Alternatives Considered

- **A more aggressive Android `minSdk` (e.g. 31 / Android 12).** Considered — it would simplify some platform code. Rejected: that is *app* thinking; for a *library*, Google's own practice (AndroidX) keeps `minSdk` low to maximise consumer reach, and reach data showed almost nothing gained by raising.
- **A lower iOS deployment target (15 / 16).** Considered. Rejected in favour of 18: it matches Apple's new-product norm, costs little reach, and leaves the largest buffer before Kotlin/Native's rising floor could force a breaking raise.
- **Treating any minimum raise as forbidden until 1.0 (strict `versioning.md` reading).** Rejected: external forces (vendor EOL, security, toolchain) can compel a raise outside our control; the managed-raise policy handles those as documented breaking releases.
- **Treating a raise as a routine per-release bump (literal `scope.md` reading).** Rejected: it understates consumer impact — `versioning.md` is right that raising is breaking.

## Related Decisions

- [ADR-017](0017-mobile-targets-and-build-stack.md) — enables the targets whose minimums this sets; `compileSdk` / SDK-build choices live there.
- [ADR-007](0007-strict-backward-compat-from-0x.md) — makes a minimum raise a major-version event.
- [ADR-016](0016-maven-coordinates-and-first-publish.md) — versioning / coordinates context.

## Related Documents

- [`../versioning.md`](../versioning.md) — "What Constitutes a Breaking Change" (platform-minimum raises) and the managed-raise policy.
- [`../scope.md`](../scope.md) — committed platform coverage (minimums) and the reconciled "may be raised" wording.
- [`../open-questions.md`](../open-questions.md) — forward-dependency reasoning (minimums set for the whole `0.x` arc).
