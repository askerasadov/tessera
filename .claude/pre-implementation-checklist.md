# Pre-Implementation Checklist

This document lists the minimum set of things that should be true before implementation work begins on this project. It is a checklist, not a process — the items are concrete and verifiable, and the checklist passes when every item is satisfied.

The purpose: to prevent implementation from starting in an unstable state. Once code is being written, fixing missing prerequisites becomes more disruptive than satisfying them upfront.

This checklist is read by AI assistants (notably Claude Code) and by the human author at the moment of transitioning from design to implementation. If any item is unchecked, address it before writing the first line of code.

---

## Project Identity

- [x] **Project name decided.** The project is named **Tessera**. The `[PROJECT_NAME]` placeholder has been replaced across all relevant files. Verify with `grep -r "\[PROJECT_NAME\]" .` returning zero results.
- [x] **Author attribution decided.** The project is published under "Asker Asadov (Lightine)" — personal name as the legal copyright holder, with Lightine as the project's brand. Reflected in the LICENSE file.
- [x] **Root namespace decided.** The Kotlin package path root is **`io.lightine.tessera`**. This appears in every Kotlin source file. Sub-package structure (e.g., `io.lightine.tessera.mrz.parsing`) emerges as code is written and can be refactored within modules.

---

## Repository Setup

- [x] **Git platform chosen.** GitHub is the chosen hosting platform. Remote not yet configured; first push deferred to a separate explicit step. See `docs/open-questions.md` (resolved entry).
- [x] **`LICENSE` file at project root.** Apache 2.0 license text with copyright attribution to "Asker Asadov (Lightine)" exists at project root.
- [x] **`.gitignore` configured.** A `.gitignore` exists at project root, derived from the planning material in `.claude/gitignore-planning.md`. Excludes IDE files, build outputs, OS-specific files, local environment files, and `SESSION-HANDOFF-*.md` working notes.
- [x] **No private content in the repository.** Verified clean by case-sensitive grep at the moment of the initial scaffolding commit. The recurring scan command lives in the AI memory system (private) for re-use before each commit.

---

## Build Configuration

- [x] **Gradle build files exist.** `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, and `gradle/gradle-daemon-jvm.properties` are present. Wrapper pinned to Gradle 8.14; daemon JVM pinned to JDK 17. Foojay resolver applied for project-level toolchain auto-provisioning.
- [x] **Module structure scaffolded.** The five core logic and cross-cutting modules (`domain`, `mrz-core`, `emrtd-core`, `telemetry`, `logging`) exist as Gradle modules with `build.gradle.kts`. Platform I/O and UI modules are deliberately deferred to their corresponding releases per `docs/open-questions.md`; the empty-shell cost is not justified before the first implementation work in each.
- [x] **Initial dependencies declared.** Kotlin Multiplatform plugin, Spotless, ktlint, kotlin.test, and Kotest property module are declared via the version catalog. Inter-module dependencies (`mrz-core`/`emrtd-core` → `domain`, `logging`) are wired per the architecture graph.
- [ ] **Target platforms configured.** Only the JVM target is configured on the five scaffolded modules. Android (API 26+) and iOS (15.0+) targets are deferred — Android until 0.2.0 work begins, iOS until Xcode is installed. Both deferrals are tracked in `docs/open-questions.md` (Principle 2: don't add infrastructure before it earns its keep).

---

## Tooling

- [ ] **IDE configured.** Pending user action. IntelliJ IDEA with the KMP plugin is the documented choice; opening the project for the first time is a manual step the user performs when they next pick up implementation work.
- [x] **Code style tooling chosen.** Spotless (root-level) with a ktlint backend, configured against `.editorconfig` and the Kotlin official style. `./gradlew spotlessCheck` runs as part of `./gradlew build`. See `docs/conventions.md` Code Style section.
- [x] **Test framework chosen.** `kotlin.test` is wired into every module's `commonTest` source set.
- [x] **Property-based testing library chosen.** Kotest property module (`io.kotest:kotest-property`) is wired into every module's `commonTest` source set, ready for the round-trip property tests committed to in `docs/testing.md`.

---

## Documentation Alignment

- [x] **`docs/conventions.md` updated.** Code Style section now describes Spotless + ktlint + `.editorconfig` + Kotlin official style.
- [x] **`docs/open-questions.md` updated.** "Code style tooling" and "Git platform choice" marked Resolved. Three new deferrals added (iOS target, Android target, platform I/O and UI module scaffolding) with explicit triggers for resolution.
- [x] **No stale forward references.** Verified at the moment of the initial scaffolding commit. The four matches that remain are legitimate meta-references in `.claude/known-pitfalls.md` and `.claude/pre-implementation-checklist.md`, plus prose in `docs/architecture.md:195` ("only the I/O bridge needs to be written") that is future-tense narrative, not a stale doc forward-reference.
- [x] **Cross-reference check tooling configured.** `scripts/check-cross-references.sh` is committed and executable. Run from project root. Pre-commit hook is a future improvement, not blocking.

---

## Verification

- [x] **`gradlew build` succeeds.** Confirmed — `./gradlew clean build` reports BUILD SUCCESSFUL. Note: Gradle 8.14 emits a deprecation warning about Gradle 9.0 compatibility from the Kotlin Multiplatform plugin; this is upstream and non-blocking.
- [x] **A trivial test runs.** `mrz-core/src/commonTest/kotlin/io/lightine/tessera/mrz/TestInfrastructureSmokeTest.kt` runs and passes (`./gradlew :mrz-core:jvmTest`).
- [x] **Cross-references verified.** `bash scripts/check-cross-references.sh` reports "All cross-references resolve."

---

## When the Checklist is Complete

When every item is checked:

1. The project is in a known-good initial state
2. Implementation can begin against the documented architecture and feature specifications
3. The first session of implementation work has a clean starting point — no surprises about missing setup

Implementation typically starts with `mrz-core` (per the release roadmap in `docs/scope.md`): the data model first, then parsing, generation, validation, lookup tables, transliteration. Each implementation session can use the session handoff template in `.claude/session-handoff-template.md` to leave a clean state for the next session.

---

## Maintaining This Checklist

If a new prerequisite emerges during the design or early implementation phase, add it here. If an item turns out to be unnecessary, remove it. The bar for items: concrete, verifiable, and genuinely needed before writing implementation code.

Items that turn out to be optional (nice to have but not blocking) should be moved to a separate document or removed entirely. This checklist is a gate, not a wishlist.
