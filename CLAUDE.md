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

1. Look for the most recent `SESSION-HANDOFF-YYYY-MM-DD.md` at the project root. If one exists, read it before engaging with the user's request.
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

- **At the end of a substantive working session**, write a session handoff file at the project root named `SESSION-HANDOFF-YYYY-MM-DD.md`, using the template in [`.claude/session-handoff-template.md`](.claude/session-handoff-template.md). A "substantive" session means: more than a small fix, decisions made that affect future work, or work stopped mid-task.
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
- **Update `CHANGELOG.md` for every release.** Keep a Changelog format.
- **Read the relevant ADR before changing established ground.**

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
