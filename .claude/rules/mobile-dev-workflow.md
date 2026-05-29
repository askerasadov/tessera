---
paths:
  - "mrz-camera-*/**"
  - "**/androidMain/**"
  - "**/iosMain/**"
  - "**/androidInstrumentedTest/**"
  - "**/androidUnitTest/**"
  - "**/iosTest/**"
---

# Mobile Development Workflow

This rule loads when working on Tessera's mobile (Android / iOS) code. It defines **how mobile development is driven** here. The human-facing setup counterpart is [`docs/development-setup.md`](../../docs/development-setup.md).

## Drive everything from the command line / agent tooling

- **Android** — drive via Google's **Android CLI** (the agent-optimized tool wrapping `sdkmanager` / `avdmanager` / `adb`) plus its Skills and Knowledge Base. Use `./gradlew` for builds and tests.
- **iOS** — drive via the **Xcode MCP** (`mcpbridge`): build, test, run, diagnostics.

## Inspect state as text — including the screen

Read device, app, and *screen* state as **text**, never as an image:
- Logs / results: `adb logcat`, test output, Gradle output.
- UI / screen state: `uiautomator dump` (the view hierarchy as text) on Android; Xcode MCP diagnostics on iOS.
- When you need to "see what's happening," reach for a text dump — not a screenshot.

This is complete on purpose: the command line surfaces every piece of state you need as text, so there is no situation that requires capturing an image.

## The one hard border (enforced)

A screenshot pulled into an assistant's context can blow past image-size limits and **destroy the whole session's context** — this has happened and cost a full session. So `screencapture` / `adb … screencap` / `adb … screenrecord` / `xcrun simctl io … screenshot` are **blocked for the assistant** by a PreToolUse hook in [`.claude/settings.json`](../settings.json) (via [`scripts/block-screenshot.sh`](../../scripts/block-screenshot.sh); no override). The human's own terminal is unaffected — if a screenshot is ever genuinely needed, the human takes it. The positive method above already keeps you away from this border; the hook is the enforcement so habit can't reintroduce it. *(Prescribe the path; prohibit — and enforce — the border.)*

## Testing layers (no live camera on simulators)

- **Host (JVM)** — glue logic tested with injected frames + mock OCR; no device.
- **Emulator / Simulator** — real OCR (ML Kit / Apple Vision) on a *still image*. The iOS Simulator has **no camera**; Vision still runs on a supplied image.
- **Physical device** — the only place a live lens is validated end-to-end.

## Cross-references
- Human setup + toolchain: [`docs/development-setup.md`](../../docs/development-setup.md).
- Document map: [`CLAUDE.md`](../../CLAUDE.md).
