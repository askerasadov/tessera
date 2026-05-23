---
paths:
  - "**/*.md"
---

# Folder and File Organization

**Folder placement reflects audience.** Folders holding *project deliverables* — source code, runnable scripts, human-facing docs — live at visible paths (`docs/`, `scripts/`, source modules, root-level files like `README.md` and `CHANGELOG.md`). Folders holding *project infrastructure* — AI-facing conventions, tool config, build state, AI working notes, maintainer reference material — live at dot-prefix paths (`.claude/`, `.github/`, `.gradle/`, `.handoffs/`, `.recaps/`, `.conformance/`, `.spec/`). The dot-prefix signals "infrastructure, not deliverable," regardless of whether the contents are committed (most of `.claude/`, `.github/`) or gitignored (`.handoffs/`, `.gradle/`, `.spec/`). Gitignore status is a separate decision driven by "is this universal across clones?" (commit) vs "is this per-machine local?" (gitignore).

**Naming depends on file purpose, not folder location.** Dated working notes use `<CATEGORY>-YYYY-MM-DD[-HHMM][-<slug>].md` (UPPERCASE category prefix, which may itself be multi-word with internal hyphens — e.g., `SESSION-HANDOFF`, `RECAP-CODE-DOC-ALIGNMENT`, `CONFORMANCE-NOTES`). Evergreen documentation files use lowercase-hyphen names with the `.md` extension (real examples: [`../../docs/features/mrz-parsing.md`](../../docs/features/mrz-parsing.md), [`../../docs/features/lookup-tables.md`](../../docs/features/lookup-tables.md), [`../working-patterns.md`](../working-patterns.md)). Sequentially-numbered docs like ADRs add a 4-digit prefix to the same pattern ([`../../docs/decisions/0001-kotlin-multiplatform.md`](../../docs/decisions/0001-kotlin-multiplatform.md), [`../../docs/decisions/0007-strict-backward-compat-from-0x.md`](../../docs/decisions/0007-strict-backward-compat-from-0x.md)). Root-level project files follow long-standing software convention (UPPERCASE: `README.md`, `CHANGELOG.md`, `LICENSE`, `SECURITY.md`, `CONTRIBUTING.md`, `CLAUDE.md`).

**Scope.** Applies to *working-pattern artifacts* the project produces — categorized notes that recur and have a documented purpose — and to project documentation. Does NOT apply to *personal scratch* (one-off debugging logs, personal todos, "thinking out loud" files) or to *externally-named folders* (`build/`, `node_modules/`, `gradle/`) that follow their tool's own conventions.

**Self-healing.** Hidden working-note folders (`.handoffs/`, `.recaps/`, `.conformance/`, etc.) materialize on first use — a session writing the first file creates the parent directory if absent. No setup script needed.

## Cross-references

- Project-level summary lives in [`../../CLAUDE.md`](../../CLAUDE.md) under "Folder and File Organization."
- Human-facing summary in [`../../docs/conventions.md`](../../docs/conventions.md) under "Folder and File Organization."
