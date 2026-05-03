# `.claude/` Folder

This folder contains material specifically for AI assistants (primarily Claude Code) working on this project. The content is intentionally separate from `docs/` because it serves a different audience and a different purpose.

`docs/` is for humans (and AI) understanding the project itself.
`.claude/` is for AI assistants understanding how to work effectively *on* the project.

The entry point for AI assistants is the `CLAUDE.md` file at project root. That file is the orientation document and should be read first. The files in this folder are deeper material loaded as needed:

- **`working-patterns.md`** — Concrete patterns for how work happens on this project: technical patterns and collaboration patterns. Read when starting substantive work.
- **`known-pitfalls.md`** — Real failure modes that have surfaced during the project. Read before drafting significant changes.
- **`gitignore-planning.md`** — What goes into the public repo and what does not. Read before committing anything new.
- **`session-handoff-template.md`** — Template for ending a Claude Code session with a clear handoff to the next session.
- **`pre-implementation-checklist.md`** — The gate before implementation begins. Every item must be satisfied before writing the first line of code.

Material in this folder is public — it goes into the open-source repository at the 1.0.0 release. Nothing private should accumulate here. If private notes are needed during development, they live elsewhere (a personal workspace folder outside the repo) and never get committed.
