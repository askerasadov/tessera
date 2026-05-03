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

- [ ] **Git platform chosen.** GitHub, GitLab, Codeberg, or another platform has been selected. The remote is configured and the project is connected to it. (Default expectation: GitHub. See `docs/open-questions.md` for the open decision.)
- [x] **`LICENSE` file at project root.** Apache 2.0 license text with copyright attribution to "Asker Asadov (Lightine)" exists at project root.
- [x] **`.gitignore` configured.** A `.gitignore` exists at project root, derived from the planning material in `.claude/gitignore-planning.md`. Excludes IDE files, build outputs, OS-specific files, local environment files, and `SESSION-HANDOFF-*.md` working notes.
- [ ] **No private content in the repository.** A grep for organizational names, the author's location, personal information, and any private context confirms zero matches across all files. Use the rules in `.claude/gitignore-planning.md`.

---

## Build Configuration

- [ ] **Gradle build files exist.** `build.gradle.kts` and `settings.gradle.kts` are present and correctly configured for Kotlin Multiplatform.
- [ ] **Module structure scaffolded.** The modules listed in `docs/architecture.md` (`mrz-core`, `emrtd-core`, `domain`, `telemetry`, `logging`, plus platform I/O and UI modules as appropriate) exist as Gradle modules. They do not need to contain implementation yet, but their build configuration must be in place.
- [ ] **Initial dependencies declared.** Kotlin standard library, KMP plugins, and any libraries identified during design (e.g., test frameworks) are declared in build files.
- [ ] **Target platforms configured.** Android (API level 26+) and iOS (15.0+) targets are configured per ADR-001 and ADR-002.

---

## Tooling

- [ ] **IDE configured.** IntelliJ IDEA (or chosen alternative) opens the project successfully. KMP plugin is installed and recognized.
- [ ] **Code style tooling chosen.** A formatter (e.g., ktfmt, ktlint) and linter have been chosen and configured. The project compiles with the chosen style applied.
- [ ] **Test framework chosen.** A test framework (e.g., kotlin.test, JUnit 5) has been chosen and is configured for `commonMain` test sources.
- [ ] **Property-based testing library chosen.** A property-based testing library (e.g., Kotest property module) has been chosen if property-based tests are in scope from the start. (See `docs/testing.md`.)

---

## Documentation Alignment

- [ ] **`docs/conventions.md` updated.** The "Code Style" section now reflects the actual chosen tooling, replacing the placeholder language about deferred decisions.
- [ ] **`docs/open-questions.md` updated.** Items resolved by the items above (project name, root namespace, git platform, code style, distribution preparation) are marked Resolved with reference to where they were decided.
- [ ] **No stale forward references.** A grep for `when written`, `will be written`, `to be written`, `to be created` returns no matches in the documentation (excluding example text in `.claude/known-pitfalls.md` and similar meta-references).
- [ ] **Cross-reference check tooling configured.** A simple script or pre-commit hook checks that every `.md` cross-reference in the documentation resolves to an existing file. Catches broken links as docs evolve. The script can be a one-liner: `grep -roh "\`[a-z][a-z0-9-]*\.md\`" --include="*.md" | sort -u`, with a corresponding existence check.

---

## Verification

- [ ] **`gradlew build` succeeds.** Or whatever the equivalent build command is for the chosen tooling. The empty project builds without errors.
- [ ] **A trivial test runs.** A "hello world" style test in `commonMain` test sources passes. This confirms the test infrastructure is functional before real test writing begins.
- [ ] **Cross-references verified.** All `.md` cross-references in the documentation resolve to existing files.

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
