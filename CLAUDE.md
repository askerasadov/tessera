# CLAUDE.md

This file orients AI assistants — particularly Claude Code — picking up this project. Read this first; load deeper material from `docs/` and `.claude/` as needed.

If you are a human contributor, [`README.md`](README.md) is the better starting point.

---

## Project at a Glance

Tessera is a vendor-neutral SDK for reading, validating, and generating identity document data — primarily MRZ from passports, ID cards, residence permits, and visas conforming to ICAO Doc 9303. NFC chip reading and other capabilities are planned for later releases.

**Current state:** Design complete, implementation pending. Foundation, architecture, scope, conventions, versioning, feature documentation, ADRs, and supporting material are all written. The first implementation work has not yet begun.

---

## What to Do First

If you are starting a new session:

1. Look for the most recent `SESSION-HANDOFF-YYYY-MM-DD-HHMM-<slug>.md` at the project root. The current convention is date + UTC time (4 digits, no separator) + kebab-case slug, so `ls -1 SESSION-HANDOFF-*.md | sort -r | head -1` returns the canonical latest within the new form. The `YYYY-MM-DD` prefix ensures date order across all forms; within any single date, all handoffs share the form current at that time, so within-date mixing is not expected. Older dates may use either legacy form — `SESSION-HANDOFF-YYYY-MM-DD-<slug>.md` (date + slug, no time) or `SESSION-HANDOFF-YYYY-MM-DD.md` (date only, no slug). Treat all forms the same when reading.
2. If no handoff exists, briefly skim [`docs/principles.md`](docs/principles.md) to refresh the foundational stance (10 min).
3. Check [`docs/open-questions.md`](docs/open-questions.md) for what is currently in flight.
4. Engage with the user's request.

Do not draft major changes without first understanding what is already in place. The documentation captures decisions that are easy to accidentally undo.

---

## Document Map — Where to Find Things

If you are looking for...

| You need... | Look at... |
|---|---|
| The foundational principles | [`docs/principles.md`](docs/principles.md) |
| What the SDK supports / does not support | [`docs/scope.md`](docs/scope.md) |
| Module structure and dependencies | [`docs/architecture.md`](docs/architecture.md) |
| How decisions get made and how docs are written | [`docs/conventions.md`](docs/conventions.md) |
| Versioning and release rules | [`docs/versioning.md`](docs/versioning.md) |
| Testing discipline | [`docs/testing.md`](docs/testing.md) |
| The risk profile of each reading method | [`docs/reading-risks.md`](docs/reading-risks.md) |
| Definitions of MRZ, BAC, PACE, etc. | [`docs/glossary.md`](docs/glossary.md) |
| What is currently deferred or in flight | [`docs/open-questions.md`](docs/open-questions.md) |
| Why a significant decision was made | [`docs/decisions/`](docs/decisions/) |
| A specific feature's design | [`docs/features/`](docs/features/) |
| Concrete working patterns | [`.claude/working-patterns.md`](.claude/working-patterns.md) |
| Failure modes to avoid | [`.claude/known-pitfalls.md`](.claude/known-pitfalls.md) |
| What goes in the public repo and what doesn't | [`.claude/gitignore-planning.md`](.claude/gitignore-planning.md) |
| Session handoff template | [`.claude/session-handoff-template.md`](.claude/session-handoff-template.md) |
| Pre-implementation gate checklist | [`.claude/pre-implementation-checklist.md`](.claude/pre-implementation-checklist.md) |
| Git and GitHub workflow (branch naming, PR flow, gh CLI usage) | [`.claude/git-workflow.md`](.claude/git-workflow.md) |

---

## The Principles That Matter Most

Full list in [`docs/principles.md`](docs/principles.md). The four that shape day-to-day work most directly:

- **Principle 1 — Reader, not oracle.** The SDK extracts data verbatim and reports observations. It does not make trust decisions. No `isValid` boolean, no auto-correction, no inference of intent.
- **Principle 4 — Honest about what we know.** Distinguish settled facts from current decisions from open questions from speculation. Do not pretend certainty you do not have.
- **Principle 5 — Transparency.** If the SDK extracts data internally, it exposes it externally. Raw values are exposed alongside computed values. Nothing is hidden.
- **Principle 11 — Internal packages first.** New features start as internal packages within existing modules. Promote to standalone module only when justified.

When tempted to do something convenient, check whether you are crossing into oracle territory. If yes, stop.

---

## Rules

The operational ruleset. Each rule is concrete and triggers on a specific situation.

### Session Discipline

- **At the end of a substantive working session**, write a session handoff file at the project root named `SESSION-HANDOFF-YYYY-MM-DD-HHMM-<slug>.md`, where the time component is the current UTC time as four digits with no separator (e.g., `0930`, `2256`) and `<slug>` is a short kebab-case summary of what shipped (match the feature branch's slug when there is one — e.g., `validator`, `expiry-warnings`, `explicit-api`). Use the template in [`.claude/session-handoff-template.md`](.claude/session-handoff-template.md). A "substantive" session means: more than a small fix, decisions made that affect future work, or work stopped mid-task. Both the time and the slug are mandatory: the time makes `ls | sort -r` deterministic without depending on filesystem mtime (which gets clobbered by `git clone`, `rsync`, archive extraction, etc.); the slug makes the directory listing self-documenting at a glance.
- **At the start of a session**, look for the most recent handoff file and read it first.
- **Use `/clear` between distinct tasks** to reset context.
- **Use `#` to capture recurring instructions** so they persist across sessions.

### Forbidden Actions

- **Never commit credentials, secrets, API keys, tokens, or private content.**
- **Never name specific organizations** in any committed file (no employer organizations, no anchor users, no business partners). Standards bodies (ICAO, ISO/IEC) and authorities issuing specific laws may be cited for accuracy.
- **Never include real document data** in tests, fixtures, or examples. Synthetic data only, generated by the SDK's own generator. ICAO-published test vectors where applicable.
- **Never commit `SESSION-HANDOFF-*.md` files.** They are working notes; the `.gitignore` excludes them.
- **Never auto-correct or auto-infer trust decisions** in the SDK code. Reader, not oracle.
- **Never embed version numbers in feature documentation body text.** Use structural markers (`Available since X.Y.Z`) only.

### Required Discipline

- **Tests for every new public API.** No public API ships without a test that exercises documented behavior.
- **Tests alongside implementation, not after.** Write the test as you build the feature, not as a follow-up.
- **Tests for every new error type.** When adding a new error to the taxonomy, add a test that produces it.
- **When tests reveal an error condition that has no type yet, add it to [`docs/features/mrz-error-taxonomy.md`](docs/features/mrz-error-taxonomy.md) and write a test that produces it.** The error taxonomy grows through discovery.
- **Update `CHANGELOG.md` for every non-trivial PR.** Keep a Changelog format. Entries go under `[Unreleased]`, grouped Added / Changed / Deprecated / Removed / Fixed / Security. Trivial PRs (single-character typo fixes etc.) can skip with a one-line explanation in the PR description. Detail in [`.claude/git-workflow.md`](.claude/git-workflow.md).
- **Read the relevant ADR before changing established ground.**

### Dependency Upgrade Cadence

The project bumps the toolchain and dependencies to current stable on a **six-monthly cadence**. This keeps the project from accumulating multi-year-stale tooling (which compounds upgrade pain) while not chasing every patch release (which is its own form of churn).

- **Next scheduled check: 2026-10-01.** After that, every six months: 2027-04-01, 2027-10-01, 2028-04-01, etc.
- The exact day doesn't matter — anything within ±2 weeks of the date is fine. The cadence is the operational rhythm, not a hard deadline.
- **What to bump each cycle:** Kotlin (and KMP plugin), Gradle wrapper, JDK toolchain floor, dev tooling (Spotless, ktlint), runtime dependencies (kotlinx-datetime, etc.), test dependencies (kotest), and any other plugins pinned in `settings.gradle.kts` (foojay-resolver-convention, etc.). The complete inventory is whatever has a version pinned in `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts`, and the `jvmToolchain(N)` calls across module `build.gradle.kts` files.
- **How to bump:** verify the latest stable of each via web search at upgrade time (knowledge cutoffs drift); check the compatibility matrix between Kotlin + Gradle + KGP; bump to the highest mutually supported triple. JDK toolchain bump is a separate call — only move when the LTS situation warrants it and the build/test surface is stable.
- **Split into 2–3 focused PRs per cycle.** The first cycle (2026-05-17) used three PRs: (1) Kotlin + KMP + Gradle + JDK toolchain, (2) dev tooling (Spotless + ktlint) + Gradle wrapper regeneration, (3) runtime + test dependencies + this cadence rule + docs. The split keeps blast radius small if any single bump breaks something.
- **Cross-reference from [`docs/conventions.md`](docs/conventions.md)** so human contributors see the cadence too.

### Pre-Release Tech-Stack Review

Before starting work on each `0.x` (or later `x.0` / `x.y` minor) release, the project does an explicit tech-stack review. The review is milestone-driven and complementary to the clock-driven Dependency Upgrade Cadence above. Where the cadence asks "are we on current stable?", the review asks "are our underlying choices still right for what we're about to build?"

- **Trigger: before any code is written for the next release.** Not before tagging — pre-tag is "verify we shipped what we said" (the recap pattern). Pre-start is when the discussion has maximum leverage; once code is being written, retroactive tech-stack changes are expensive.
- **Scope of the review:** revisit foundational architectural choices (KMP, native UI per platform, sealed type hierarchies, etc.) against the upcoming release's actual demands; identify new dependencies the upcoming subsystem will need (e.g., camera library for 0.2.0, NFC library for 0.6.0, transliteration approach for 0.1.0); identify local-machine tooling the build can't auto-provision (platform SDKs, CLIs, test hardware — e.g., Android command-line tools + Android SDK for 0.2.0, Xcode + simulators for iOS, an NFC-capable test device for 0.6.0); flag any API-stability commitments the release would lock in that should be reconsidered first; surface tech-stack drift that the 6-monthly cadence didn't catch (e.g., a foundational tool reaching end-of-life).
- **Output:** a brief decision record (could be a new ADR if significant, or a recap-style working note if smaller). At minimum, name what was reviewed and what was decided. The output names *project* expectations — how each contributor satisfies local-tooling prerequisites on their own machine is theirs to track (e.g., via personal notes or AI-assistant memory). The 2026-05-17 pre-0.1.0 recap is the working precedent for this format.
- **What this is not:** not a full design review of the upcoming release. The release's own design lives in the relevant feature docs and gets developed slice-by-slice as usual. The tech-stack review is the narrow architectural-fit pass that gates the start of release work.
- **Cross-reference from [`docs/conventions.md`](docs/conventions.md)** so human contributors see the convention too.

### Git and GitHub Workflow

The project uses **GitHub Flow**: `main` is the trunk; feature branches off `main`; PR for every merge. Detail in [`.claude/git-workflow.md`](.claude/git-workflow.md).

- **Branch off `origin/main`** for every PR. Name `feature/...`, `fix/...`, `docs/...`, or `chore/...` per the contents.
- **Rename auto-generated worktree branches** before pushing. `claude/<random>` is meaningless to reviewers.
- **Run the private-content scan before every push** to a public-or-soon-to-be-public remote. Grep terms are in memory `feedback_private_content_scan.md`.
- **Use the PR template** at `.github/pull_request_template.md`. Fill Documentation Impact, Tests, Open Questions, Changelog, and Verification sections.
- **`gh` CLI is set up and authed** in this environment. Push and create PRs directly from the session.
- **Delete merged branches locally** with `git branch -d <name>` after merge. Remote branches auto-delete via repo settings.

### Documentation Sync (When You Change X, Update Y)

- **When changing a public API in code**, update the relevant feature document in [`docs/features/`](docs/features/) so illustrative shapes match the actual API.
- **When making a significant decision**, draft a new ADR in [`docs/decisions/`](docs/decisions/) and add it to the index.
- **When implementation reveals a doc gap or contradiction**, surface it and update the doc — do not silently work around it.
- **When adding a new test category or testing commitment**, update [`docs/testing.md`](docs/testing.md).
- **When resolving an item in [`docs/open-questions.md`](docs/open-questions.md)**, mark it Resolved with a reference to where the resolution was made. Do not delete.
- **When deferring a new decision**, add an entry to [`docs/open-questions.md`](docs/open-questions.md).

### Verification Before Acting

- **Before drafting major changes**, confirm the change does not contradict an established decision.
- **Before introducing a new dependency**, confirm it is justified. Each adds a maintenance burden.
- **Before writing more than ~50 lines of new content without checking in**, pause and verify direction with the user. Short cycles produce better outcomes.
- **Before committing to a foundational decision**, verify alignment with primary sources — the actual committed docs (`scope.md`, ADRs, `open-questions.md`, feature docs) — not derived sources (recaps, summaries, prior interpretations) that can drift. Foundational decisions are anything ADR-007 backward-compatibility will lock at `0.1.0` (tech-stack choices, scope-defining wording, architectural commitments). The trigger is *cost of being wrong is high*, not *I want to feel thorough*. Full pattern in [`.claude/working-patterns.md`](.claude/working-patterns.md) under "Pre-commitment alignment check"; complements the "trust the doc system" stance by naming its exception.

### Resisting "Looks Ready"

A known failure mode: declaring work "ready" prematurely, only to find gaps under follow-up questioning.

- **Be skeptical of your own "ready" judgments.** Before declaring something complete, ask: "what would I check if I were skeptical of my own work?"
- **Bias toward acknowledging incompleteness.** A doc set is more often *almost* ready than fully ready. Surface what you are uncertain about rather than papering over it.
- **Treat the user's follow-up questions as signal, not friction.** When they ask "did we cover X?" they are often right that you did not.
- **A "no issues found" verification is suspicious.** If a check produces zero findings, double-check the check itself before trusting the result.

---

## Maintaining the Documentation System

When new content needs a home, use this guidance:

| New content is... | Goes in... |
|---|---|
| A rule that applies in every session | This file (CLAUDE.md, Rules section) |
| A working pattern observed during implementation | [`.claude/working-patterns.md`](.claude/working-patterns.md) |
| A mistake to avoid | [`.claude/known-pitfalls.md`](.claude/known-pitfalls.md) |
| A deferred decision | [`docs/open-questions.md`](docs/open-questions.md) |
| A significant architectural or scope decision | A new ADR in [`docs/decisions/`](docs/decisions/) |
| A new domain term | [`docs/glossary.md`](docs/glossary.md) |
| A new API or capability | The relevant feature doc in [`docs/features/`](docs/features/) |
| A testing commitment | [`docs/testing.md`](docs/testing.md) |
| A risk profile for a new reading method | [`docs/reading-risks.md`](docs/reading-risks.md) |

Some rules and patterns serve **multiple audiences** — both AI assistants and human contributors. When that's the case, place the content in its primary home and cross-reference from secondary homes. The established precedent in this project: rules in CLAUDE.md that also affect human contributors get a short cross-reference subsection in [`docs/conventions.md`](docs/conventions.md). The Dependency Upgrade Cadence, Pre-Release Tech-Stack Review, and Pre-commitment alignment check rules are the working precedents. Before placing a new rule, ask: *who does this serve — future AI sessions, future human contributors, or both?* Place and cross-reference accordingly. Memories serve a narrower audience (one Claude on one machine) and are appropriate when the content is genuinely local; rules that apply to any contributor or any AI on the repo belong in committed files instead.

After every substantive session, ask: *did this session reveal anything that should be added to the documentation? Was anything in the docs wrong relative to what was built?* If yes, update before writing the handoff.

---

## Working Style

The user has been clear about how they want collaboration to work:

- **Honest engagement over reflexive agreement.** Push back with reasoning when warranted. Hold positions when confident.
- **Show reasoning, not just conclusions.** Trade-offs named explicitly. The pattern *Decision I'd make / Why I'm leaning this way / Where you might disagree* surfaces reasoning before it becomes a fait accompli.
- **Distinguish "your lean" from "your call."** When the user defers to you on something they could legitimately have an opinion on, check whether they have an underlying preference. Decisions about their project, identity, or business choices belong to them.
- **Acknowledge uncertainty honestly.** Say "I don't know" or "let me check" rather than fabricating certainty.
- **Treat the user as a peer.** Different powers, equal standing. Servile collaboration produces worse output than peer collaboration.

Detailed working patterns are in [`.claude/working-patterns.md`](.claude/working-patterns.md).

---

## Related Documents

- [`README.md`](README.md) — project front door (better starting point for human readers)
- [`docs/principles.md`](docs/principles.md) — the foundational principles
- [`docs/conventions.md`](docs/conventions.md) — how work is done
- [`.claude/`](.claude/) — supporting working notes for AI assistants
