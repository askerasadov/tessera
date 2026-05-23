# Git and GitHub Workflow

This document captures the operational detail of how code lands in this project. The critical rules are summarized in [`CLAUDE.md`](../CLAUDE.md); the steps and command-level detail live here.

The branching model is **GitHub Flow**: `main` is the trunk and is always shippable. Feature work lives in short-lived branches off `main`, returns via pull request, gets reviewed, and merges back. There is no long-lived `develop` branch, no release branches, no hotfix branches. A future need (e.g., maintaining 0.x while 1.x is in development) can introduce one if it earns its keep, not before.

---

## Branch Naming

Branches are named for what they contain:

- **`feature/<short-description>`** — new functionality, e.g., `feature/validator`, `feature/td2-parser`.
- **`fix/<short-description>`** — bug fixes, e.g., `fix/check-digit-overflow`.
- **`docs/<short-description>`** — documentation-only changes, e.g., `docs/align-illustrative-shapes`.
- **`chore/<short-description>`** — build, tooling, dependency bumps, repository maintenance.

Names are descriptive enough that a reviewer reading the GitHub PR list knows what the branch is about without opening the PR.

### Worktree Default Branches

Claude Code worktrees auto-generate branch names like `claude/confident-albattani-6f0935`. **Rename before pushing** — the auto-generated name is meaningless to reviewers. From inside the worktree:

```sh
git branch -m claude/confident-albattani-6f0935 feature/<descriptive-name>
```

The worktree directory keeps its original name; only the branch reference changes.

---

## Per-PR Workflow

The standard flow when starting work on a new feature or fix:

### 1. Create the branch off latest `main`

```sh
git fetch origin
git checkout -b feature/<descriptive-name> origin/main
```

If you're in a worktree that already has a branch checked out, either rename that branch (above) or create a new one off `origin/main`.

### 2. Make the changes

Edit code, write tests, update relevant feature docs and ADRs as commitments are made or discovered. Per [`CLAUDE.md`](../CLAUDE.md) Documentation Sync rules, doc updates are part of the slice, not a follow-up.

### 3. Update `CHANGELOG.md`

Every non-trivial PR adds entries to the `[Unreleased]` section, grouped per [Keep a Changelog](https://keepachangelog.com/) categories: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`. Trivial PRs (typo fixes, single-character edits) can skip the changelog with an explanation in the PR description.

When `0.1.0` is tagged, the `[Unreleased]` section becomes `[0.1.0] - YYYY-MM-DD` and a new `[Unreleased]` section is opened.

### 4. Run the private-content scan

**Required before every push to a public-or-soon-to-be-public remote.**

For Claude Code sessions, this is automated. The PreToolUse hook in `.claude/settings.json` fires on every `Bash(git push *)` call and runs `scripts/private-content-scan.sh`. If the script exits non-zero, the push is blocked and the offending lines surface as the block reason. You don't need to remember to run it — the hook does.

For manual pushes (from a regular terminal), run the script yourself before pushing:

```sh
bash scripts/private-content-scan.sh
```

The script reads scan terms from `.claude/private-content-terms.local` (gitignored — each contributor maintains their own; see "First-time setup" below). It greps `git ls-files`-tracked `.md`, `.kt`, and `.gradle*` files for case-insensitive substring matches, then filters out the documented false positives.

Expected exit codes:

- **0** — `no private content found in tracked files.` Proceed.
- **0** with `terms file not found ... skipping.` — no terms configured locally; the scan no-ops. See "First-time setup" below.
- **2** — `BLOCKED — possible private content found...` Stop and inspect the listed lines.

#### First-time setup

Create `.claude/private-content-terms.local` with one substring per line. Lines starting with `#` are comments; blank lines are skipped. The file is gitignored and stays on your local machine only. The terms themselves are the private context being protected, so the file must never be committed (`.gitignore` already excludes it).

#### Documented false positives

The script's `FALSE_POSITIVES` extended-regex excludes lines matching any of:

1. **`InvalidData`** in `docs/features/mrz-error-taxonomy.md` — generic English compound; substring matches the scan but the word is not private context.
2. **`"Azerbaijan"`** (with the surrounding double quotes) in `mrz-core/src/commonMain/.../recognition/CountryCodeTable.kt` — the ISO 3166-1 alpha-3 English short name, standard data identifier treated identically to USA / GBR / DEU / etc. The convention is about prose / comments / docs, not standard data identifiers.
3. **`azerbaij.pdf`** in URL anchors pointing to the Library of Congress ALA-LC romanization document (`https://www.loc.gov/catdir/cpso/romanization/azerbaij.pdf`) — primary citation source for the AZE transliteration profile. The filename is what LoC chose; the URL is a citation, not authored prose. Used in `docs/decisions/0009-transliteration-profiles.md`, `docs/features/transliteration.md`, `docs/open-questions.md`.

If a NEW generic substring needs to be allowlisted, update `FALSE_POSITIVES` in `scripts/private-content-scan.sh` AND document it here in the same PR.

### 5. Run verification

Doc-only PRs:

```sh
bash scripts/check-cross-references.sh
```

Code PRs:

```sh
./gradlew spotlessApply build
bash scripts/check-cross-references.sh
```

The build runs spotless, ktlint, all unit tests, and property tests. All must pass before push.

### 6. Commit

Per the existing commit-message style: short imperative subject, blank line, body explaining the *why*. Use HEREDOC for the body to preserve formatting. Always include the Co-Authored-By trailer for commits Claude makes:

```sh
git commit -m "$(cat <<'EOF'
Short imperative subject

Body explaining what changed and why. The why is more valuable than
the what; the diff already shows the what.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

### 7. Push

```sh
git push -u origin feature/<descriptive-name>
```

The `-u` sets upstream tracking so subsequent pushes work without arguments.

The `gh` CLI is set up as a Git credential helper in this environment (via `gh auth setup-git`), so HTTPS pushes work without prompting for credentials.

### 8. Open the PR

```sh
gh pr create --base main --head feature/<descriptive-name> \
  --title "<concise PR title>" \
  --body "$(cat <<'EOF'
<PR description following the template at .github/pull_request_template.md>
EOF
)"
```

The template has these sections:

- **Summary** — 1-3 sentences on what the PR does and why.
- **Documentation Impact** — every doc the PR touches or should touch.
- **Tests** — what was added, total count, any flakiness.
- **Open Questions Touched** — items in `docs/open-questions.md` resolved, modified, or added.
- **Changelog** — confirm `CHANGELOG.md` `[Unreleased]` is updated.
- **Verification** — checklist (`./gradlew build`, cross-references, private-content scan, branch name).

When pushing from `gh pr create` directly, the template body needs to be passed via `--body`. The template at `.github/pull_request_template.md` is what fills in if a PR is opened via the web UI; for CLI creation, paste the relevant sections in `--body`.

### 9. Review

If a reviewer is involved, request review via `gh pr edit <pr> --add-reviewer <handle>` or assign on GitHub. Address comments via follow-up commits on the branch (don't squash-rebase mid-review unless requested — preserves comment threads against specific commits).

### 10. Merge

Merge strategy preferences (default is GitHub repo settings; can be configured per-PR):

- **Rebase and merge** — preserves individual commits on `main`, linear history. Recommended when commits are well-titled and tell a coherent story.
- **Squash and merge** — collapses to one commit on `main`, hides intermediate steps. Recommended when commits are messy or "WIP" style.
- **Merge commit** — preserves all commits + a merge commit indicating where they joined. Useful for clearly bounded feature branches.

After merge:

```sh
# locally, off the now-deleted feature branch:
git checkout -b <next-branch-name> origin/main   # new work
# or
git switch main && git pull                      # back to trunk
git branch -d feature/<descriptive-name>         # delete the merged local branch
```

GitHub auto-deletes the remote branch if "Automatically delete head branches" is enabled in the repo settings.

---

## Worktree Cleanup

If the work was done in a Claude Code worktree, the worktree directory remains after the branch is deleted. Clean up from outside the worktree:

```sh
# from main checkout (not from inside the worktree):
git worktree remove .claude/worktrees/<worktree-name>
```

Or let Claude Code clean up automatically when the next session creates a new worktree (default behavior).

---

## Notes for Future AI Sessions

- **`gh` CLI is installed and authed** in this user's environment as of 2026-05-04. Future sessions can push and create PRs from the session itself; no need to ask the user to do it manually unless something has broken.
- **`gh auth setup-git`** is the bridge between Git's HTTPS push and `gh`'s credentials. Without it, `git push` will hit "could not read Username for 'https://github.com'" even when `gh auth status` shows logged in.
- **PR template** at `.github/pull_request_template.md` auto-applies to web-UI PR creation. For CLI creation, the body is provided explicitly via `--body` — paste the template sections in.
- **CHANGELOG-on-every-PR** is the discipline. The PR template has a checkbox; future sessions should default to updating the changelog rather than treating it as optional.
- **Private-content scan** is mandatory before every push to a public-or-soon-to-be-public remote. Automated for Claude via the PreToolUse hook in `.claude/settings.json` calling `scripts/private-content-scan.sh`; manual pushes run the script directly. Scan terms live in the gitignored `.claude/private-content-terms.local` (per-contributor). Documented false positives are listed under "Run the private-content scan" above (`InvalidData` in `mrz-error-taxonomy.md`; `"Azerbaijan"` as ISO 3166-1 data in `CountryCodeTable.kt`; the `azerbaij.pdf` URL filename in ALA-LC citation links). None count as leaks.

---

## Related Documents

- [`CLAUDE.md`](../CLAUDE.md) — project root rules; this document is the operational expansion of the "Git and GitHub Workflow" section there.
- [`docs/conventions.md`](../docs/conventions.md) — naming conventions, code style.
- [`docs/versioning.md`](../docs/versioning.md) — Keep a Changelog format and SemVer rules.
- `.github/pull_request_template.md` — auto-applied PR description template.
- `scripts/private-content-scan.sh` — runs the private-content scan; reads terms from the gitignored `.claude/private-content-terms.local` (per-contributor). Called automatically by the PreToolUse hook in `.claude/settings.json` and runnable manually before any push.
