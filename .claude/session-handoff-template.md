# Session Handoff Template

This document provides a template for ending a Claude Code session with a clear handoff to the next session. It exists because Claude Code reads the project from scratch each session. Without explicit handoffs, context fades and progress is rebuilt from inference rather than knowledge.

The template is unusual but proven useful: investing five minutes at the end of a session to leave a structured handoff saves substantial time at the start of the next session. The next instance of Claude Code reads the handoff and immediately knows where work stopped, what is in flight, and what is next.

---

## When to Use This Template

Use it at the end of any working session that:

- Made non-trivial progress (more than a single small fix)
- Stopped mid-task (the work is not complete; the next session will continue it)
- Made decisions that affect future work
- Encountered surprises that change the path forward
- Closed an item in `open-questions.md` or added a new one

For trivial sessions (a small fix, a one-question consultation), a handoff is not needed.

---

## Where to Put the Handoff

The handoff is a file at the project root, named with the date and a short kebab-case slug describing the slice:

```
SESSION-HANDOFF-YYYY-MM-DD-<slug>.md
```

The slug summarizes what the session shipped — match the feature branch's slug when there is one (`feature/mrz-validator` → `validator`, `feature/mrz-expiry-warnings` → `expiry-warnings`, `docs/handoff-filename-slug-convention` → `handoff-filename-slug-convention`). The slug is mandatory: it makes the directory listing self-documenting and prevents collisions when two sessions ship on the same calendar day.

The next session reads the most recent handoff by sorting on date first (`YYYY-MM-DD` portion), then by mtime to break ties between same-day slugs.

Older handoffs may exist in the legacy form `SESSION-HANDOFF-YYYY-MM-DD.md` (no slug). Treat them the same as slug-form handoffs; do not bulk-rename historical files.

This file is **not committed** to the repository. It is a working note. It lives on the user's machine, gets read by the next Claude Code session, and is then discarded or archived to a personal notes folder.

Do not commit handoff files. They are by their nature snapshots of in-progress work and should not become part of the project's history.

---

## Handoff Template

The template:

```markdown
# Session Handoff — YYYY-MM-DD

## What This Session Did

[A brief summary of what was accomplished. Two to four sentences.]

## What Is in Flight

[Anything that is partially done. Be specific: what file, what change, what is the open question.]

## Decisions Made

[Any decisions that affect future work. List with brief reasoning. Cross-reference any new ADRs or open-questions entries.]

## Surprises

[Anything unexpected that came up. Things that turned out harder than planned. Things that turned out easier. Things that changed the path forward.]

## Next Session Should

[The recommended next action. Specific. "Continue work on X by doing Y" or "Start fresh on Z" rather than "more work on the project."]

## Things to Watch For

[Specific cautions for the next session. Issues that might bite. Files that need attention. Tests that are flaky.]

## Open Questions Touched

[List of `open-questions.md` items that were modified, resolved, or added during this session.]
```

---

## Examples

### Example 1: Substantial progress, work continues

*The example below is fictional — it shows what a good handoff looks like, not anything that has actually happened on this project.*

```markdown
# Session Handoff — 2026-05-15

## What This Session Did

Implemented the TD3 parser including check digit validation. All ICAO Doc 9303 Part 4 specifications covered. 47 unit tests passing.

## What Is in Flight

The composite check digit handling has a TODO at `mrz-core/.../parsing/Td3Parser.kt:142` — the implementation works for normal TD3 but the long-document-number extension is not yet handled. The test for it is written but skipped (`@Ignore("composite digit with long doc number — see TODO")`).

## Decisions Made

- Used Kotlin `value class` for `MrzField` rather than enum, to allow extension in future. Documented in code comments; might be worth a brief inline note in `mrz-data-model.md`.
- Decided to put format detection logic in a separate `formats/Detection.kt` file rather than embedding in the parser. Cleaner; matches the single-source-of-truth pattern.

## Surprises

- The Apple Vision OCR engine produces different whitespace handling than ML Kit. This will matter for camera reading (release 0.2.0). Not a problem now but worth flagging in `mrz-camera-{platform}` design.

## Next Session Should

Implement the long-document-number extension in `Td3Parser.kt:142`. Tests are at `Td3ParserLongDocNumberTest.kt`, currently `@Ignore`d. Once tests pass, remove the `@Ignore` annotation and the TODO comment.

## Things to Watch For

- The `Td3Parser` constructor is currently `internal`. May want to keep it that way for API stability — only `MrzParser.parseTD3()` should be public.
- The check digit algorithm shared between TD1, TD2, TD3 is in `formats/CheckDigit.kt`. If TD1 implementation needs to change something about it, verify TD3 tests still pass.

## Open Questions Touched

- Resolved: "Specific date inference thresholds" — confirmed 130 years for max age, documented in `mrz-validation.md`.
- New: noted that Apple Vision and ML Kit have different whitespace handling — should be addressed in 0.2.0 design.
```

### Example 2: Brief session, exploratory

*The example below is fictional — it shows what a brief, research-only handoff looks like.*

```markdown
# Session Handoff — 2026-05-22

## What This Session Did

Investigated whether to use the official Kotlin coroutines library or design a target-specific async pattern. Came down in favor of coroutines.

## What Is in Flight

Nothing started. This was research only.

## Decisions Made

- Use Kotlin coroutines for async work in `commonMain`. Suspend functions in the public API where async behavior is appropriate. Swift wrappers will use `@available(iOS 13, *)` async/await interop.
- This deserves an ADR. Not yet written.

## Surprises

- KMP coroutine support is more mature than expected; the Swift interop in particular has improved significantly.

## Next Session Should

Write ADR-012 for the coroutines decision. Format from `docs/conventions.md`. Cross-reference ADR-001 (KMP) and ADR-002 (native UI). Add to `docs/decisions/README.md` index.

## Things to Watch For

- The coroutines decision interacts with the threading model conversation that was deferred during design. May want to update relevant feature docs once the ADR is written.

## Open Questions Touched

None.
```

---

## What Makes a Good Handoff

The handoff is for future-you (or future-Claude) reading it without your current context. It should answer:

- **What state is the project in?** (Specific files, specific lines, specific decisions)
- **What was I in the middle of?** (So I can continue rather than restart)
- **What did I decide that I might forget?** (To prevent redoing decisions)
- **What surprised me?** (So future-me does not stumble on the same surprise)
- **What should I do next?** (Specific action, not vague direction)
- **What might bite me?** (Specific cautions, not generic warnings)

A bad handoff says "made progress on parser, more tomorrow." A good handoff says "implemented TD3 parser through composite check digit; long-doc-number TODO at line 142; resume by implementing that and removing the @Ignore on the corresponding test."

---

## Maintaining This Template

The template can evolve. If a section consistently goes unused, remove it. If a new section is consistently needed (specific to this project), add it. The goal is to make handoffs lightweight enough that they actually get written, while substantial enough that they are useful.
