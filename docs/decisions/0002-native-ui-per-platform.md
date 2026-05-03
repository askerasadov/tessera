# ADR-002: Native UI per platform (no Compose Multiplatform)

**Status:** Accepted

---

## Context

The SDK provides optional UI modules for consumers who want a complete out-of-the-box scanner experience (camera preview, scan region overlay, status feedback, manual entry forms, NFC tap guidance). These UI modules are platform-specific by design.

Given that ADR-001 establishes Kotlin Multiplatform for shared logic, a natural follow-up question arises: should UI also be shared via Compose Multiplatform, or written natively per platform (Jetpack Compose on Android, SwiftUI with UIKit interop on iOS)?

A decision was needed because the answer shapes how much code is written per platform, what the consumer's integration experience looks like, and how the UI modules behave inside consumer applications.

---

## Decision

UI modules are written natively per platform. Android UI uses Jetpack Compose. iOS UI uses SwiftUI with UIKit interop where required.

The SDK does not use Compose Multiplatform for shared UI.

---

## Consequences

**Positive:**

- UI components inherit the host application's theming, accessibility settings, dynamic type, and platform conventions automatically. The scanner UI on iOS feels like an iOS scanner; on Android, like an Android scanner.
- Native rendering blends cleanly with the host application. There is no foreign rendering layer that consumers must accommodate.
- Platform UI frameworks evolve with their platforms; the SDK does not have to chase a separate cross-platform UI framework's evolution.
- Accessibility integration (VoiceOver, TalkBack, Dynamic Type, high-contrast modes) works through the platform's native mechanisms without translation layers.
- The volume of UI code is small (a scanner UI is not a large surface), making duplication an acceptable cost.

**Negative:**

- UI logic that conceptually applies to both platforms (state transitions, validation visualization, input handling) is implemented twice.
- Two skill sets are needed to maintain the UI: idiomatic Compose on one side, idiomatic SwiftUI on the other.
- Visual consistency across platforms must be coordinated manually rather than enforced by a shared rendering layer. (This is also a feature — see "Positive.")

**Neutral:**

- Consumers who want bespoke designs use the headless cores instead; the optional UI modules are not the only path. This bounds the cost of native duplication: it applies only to the modules where native fit matters most.

---

## Alternatives Considered

**Compose Multiplatform.** Considered seriously. Rejected because the SDK's UI runs *inside* consumer applications, not as standalone applications. A non-native rendering layer feels foreign in a host app that is otherwise native. Compose Multiplatform is a strong choice for cross-platform applications where the entire UI stack is shared; it is a less strong choice for a small embedded UI inside a native host.

**Flutter.** Not seriously considered, for the same reasons as Compose Multiplatform plus additional concerns: the rendering layer is even more distinct, the integration story for embedding inside a native host is more involved, and the Dart ecosystem is further from the SDK's other dependencies.

**A custom shared UI abstraction with native rendering on each platform.** Considered briefly. Rejected because it amounts to building a small cross-platform UI framework, which is significant engineering for marginal benefit when the UI surface is already small.

---

## Related Decisions

- ADR-001 — Kotlin Multiplatform for shared logic. The combination of "shared logic via KMP, native UI per platform" is the project's overall stance on platform sharing
- ADR-003 — modular architecture; UI modules are leaves in the dependency graph, fully optional

---

## Related Documents

- `architecture.md` — describes how UI modules sit on top of the I/O layer
- `principles.md` — Principle 8 (Native fit over cross-platform purity); this decision is a direct expression of that principle
- `scope.md` — describes the optional UI offering
