# Development Setup

This document covers the **local build and development toolchain** for Tessera — the tools you need to compile, run, and test the code, and how the project is driven on the command line.

It is the build/dev sibling of two narrower setup docs: [`contributor-setup.md`](contributor-setup.md) covers Git identity and commit/tag signing; [`publishing-setup.md`](publishing-setup.md) covers the maintainer-only release credentials. Start with `contributor-setup.md` to be able to open a PR; come here when you need to build or run the code beyond the pure-JVM core.

This document is **living** and **tiered**: you set up only the tier(s) you actually work in, and it grows as the project's platform reach expands (camera at 0.2.0, NFC at 0.6.0, future web/desktop targets).

The macOS path is the project-verified one. Linux and Windows follow the same logic for the cross-platform tiers but have not been end-to-end verified; PRs to firm them up are welcome. iOS work is **macOS-only** (Apple toolchain).

---

## Tiers — set up only what you work on

| Tier | You're working on… | Platforms | Adds |
|---|---|---|---|
| **0 — Core** | Pure logic: parsing, validation, generation (`commonMain` / JVM) | macOS · Linux · Windows | A JDK, the Gradle wrapper |
| **1 — Android** | Android camera I/O (`mrz-camera-android`) | macOS · Linux · Windows | Android SDK, the Android CLI, an emulator |
| **2 — iOS** | iOS camera I/O (`mrz-camera-ios`) | **macOS only** | Xcode, the Xcode MCP, the Simulator |

If you only touch core logic, **Tier 0 is all you need** — the mobile tiers are irrelevant to you.

---

## Tier 0 — Core (build & test the pure logic)

Universal across platforms; all you need for the parsing/validation/generation core.

- **A JDK.** The project compiles on a **JDK 21** toolchain (`jvmToolchain(21)` in every module). You do **not** need to install JDK 21 by hand: the [foojay toolchain resolver](https://github.com/gradle/foojay-toolchains) auto-provisions it on first build. The Gradle *daemon* itself runs on **JDK 17**, pinned repo-wide in [`gradle/gradle-daemon-jvm.properties`](../gradle/gradle-daemon-jvm.properties) (`toolchainVersion=17`) and also foojay-provisioned — so the Android Gradle Plugin's JDK-17 requirement (Tier 1) is already met for everyone with no per-machine pin. Expect a one-time JDK download the first time you run `./gradlew`. [Adoptium Temurin](https://adoptium.net/) is a safe default if you prefer one installed locally.
- **The Gradle wrapper** (`./gradlew`) — committed; no separate Gradle install needed.

**Verify:** `./gradlew check` (compile + tests + Spotless/ktlint) is green.

---

## Tier 1 — Android (camera I/O)

Adds the Android platform toolchain. Needed only to build/run `mrz-camera-android` and the Android targets.

### Install (macOS — project-verified)

The project drives Android work through **Google's Android CLI** — the agent-optimized tool that consolidates `sdkmanager` / `avdmanager` / `adb` and gives agents access to the Android **Skills** and **Knowledge Base** (see *How we work*). On first run it **pre-provisions a baseline SDK** at `~/Library/Android/sdk` (platform-tools, build-tools, a recent platform + system image, and the emulator), so installing the CLI is usually all you need.

```sh
# 1. The Android CLI (also pulls down a baseline SDK on first run)
brew tap android/tap
brew install android-cli            # -> /opt/homebrew/bin/android
android update                      # keep it current

# 2. Wire it to your agent and add the Android Skills + Knowledge Base
android init                        # installs the android-cli skill for detected agents
android skills add --all            # all Skills (or pass specific skill names)

# 3. An emulator AVD for instrumented tests (any API >= 26; the bundled image is recent)
android emulator create medium_phone   # Google's reference-phone profile
android emulator list                  # shows the created name, e.g. Medium_Phone_API_36.1
android emulator start <name>          # boots and waits until ready
android emulator stop  <name>
```

Skills install **user-global** (e.g. `~/.claude/skills/`), so they apply across all your projects rather than landing in this repo. Other operating systems and install methods (curl, winget, apt) are on the [official download page](https://developer.android.com/tools/agents/android-cli/download); the SDK can alternatively come from a full Android Studio install, which the CLI then just drives.

### Environment
- Point the build at the SDK with **either** `ANDROID_HOME` (and `$ANDROID_HOME/platform-tools`, `$ANDROID_HOME/emulator` on `PATH`) **or** a gitignored `local.properties` at the repo root containing `sdk.dir=/path/to/Android/sdk`. AGP auto-detects the default macOS location, but pinning it is more explicit. The CLI finds the default SDK location on its own; to point it at a non-default path, add `--sdk=/path/to/Android/sdk` to `~/.androidrc`.
- `compileSdk` tracks the latest stable Android API; `minSdk` is 23 — see [ADR-017](decisions/0017-mobile-targets-and-build-stack.md) / [ADR-018](decisions/0018-platform-minimums-and-managed-raise.md).

**Verify:** `android info` reports the SDK location; `android emulator start <name>` boots; while it runs, `adb devices` lists it as `device` and `adb version` works.

---

## Tier 2 — iOS (camera I/O) — macOS only

Adds the Apple toolchain. Needed only to build/run `mrz-camera-ios` and the iOS targets.

### Install
- **Xcode** (26.3 or later; 26.x is current). Installing the app is not enough on its own:
  - Point the active developer directory at it: `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`.
  - Accept the license: `sudo xcodebuild -license accept`.
- **The Xcode MCP** (`mcpbridge`, Xcode 26.3+) — enable it and connect it to your agent. This is how iOS build/test/run/diagnostics are driven (see *How we work*).
- **The iOS Simulator** (bundled with Xcode). Note: the Simulator has **no camera** — it can run Apple Vision on a *supplied image*, but live-camera reading needs a physical device.
- The iOS deployment target is 18; the build uses the latest iOS SDK — see [ADR-017](decisions/0017-mobile-targets-and-build-stack.md) / [ADR-018](decisions/0018-platform-minimums-and-managed-raise.md).

**Verify:** `xcodebuild -version` shows your Xcode; the MCP tools are visible to your agent; common code compiles for the iOS targets.

---

## How we work — CLI-driven, text-first

Tessera's mobile development is **driven from the command line and through agent tooling**, not by clicking through IDE GUIs: Android via the Android CLI, iOS via the Xcode MCP. Device/app/screen state is inspected as **text** — `adb logcat`, `uiautomator dump`, Simulator/Xcode diagnostics — which is reproducible, scriptable, and friendly to AI assistants working in the repo.

The AI-facing operational detail of this workflow lives in [`.claude/rules/mobile-dev-workflow.md`](../.claude/rules/mobile-dev-workflow.md) (it loads automatically when an assistant works on mobile code). This document is the *human setup* side; that rule is the *how-to-operate* side.

### Performance practices for camera work
Camera reading is throughput-sensitive, so the camera modules follow these from the start: don't run OCR on every frame (throttle / keep-only-latest); do work off the main thread; crop to the MRZ region before OCR; and release each frame buffer promptly (also a memory-hygiene / PII win).

---

## Troubleshooting

*(Grows as contributors hit and document issues. macOS-verified entries first.)*

- **`./gradlew` triggers a JDK download on first run.** Expected — foojay is provisioning the JDK 17 daemon and/or the JDK 21 toolchain. One-time.
- **AGP can't find the Android SDK.** Set `ANDROID_HOME`, or add `sdk.dir=` to a `local.properties` at the repo root (gitignored).
- **`xcodebuild` reports the command-line tools instead of Xcode.** Run `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer` and accept the license.

---

## Related documents
- [`contributor-setup.md`](contributor-setup.md) — Git identity and commit/tag signing (do this first).
- [`publishing-setup.md`](publishing-setup.md) — maintainer-only release credentials.
- [`.claude/rules/mobile-dev-workflow.md`](../.claude/rules/mobile-dev-workflow.md) — the AI-facing CLI/MCP working method.
- [`conventions.md`](conventions.md) — project conventions (cross-references this doc).
- [ADR-017](decisions/0017-mobile-targets-and-build-stack.md), [ADR-018](decisions/0018-platform-minimums-and-managed-raise.md) — build stack and platform minimums.
