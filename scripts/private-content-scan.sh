#!/usr/bin/env bash
# Scan tracked files for private-content terms and report real hits.
#
# Reads scan terms from .claude/private-content-terms.local (one per line,
# case-insensitive substring match). That file is gitignored — each
# contributor maintains their own terms locally. The terms themselves
# must NEVER appear in committed files, including this script.
#
# Ground truth is `git ls-files`, so the scan matches what a fresh cloner
# (or GitHub) sees, not the local working tree.
#
# Documented false positives are filtered out:
#   - "InvalidData" — generic English compound in
#     docs/features/mrz-error-taxonomy.md (substring matches the term
#     "idda" but is not private content)
#   - "Azerbaijan" wrapped in double quotes — ISO 3166-1 alpha-3 short
#     name in mrz-core/.../CountryCodeTable.kt (standard data identifier,
#     same treatment as USA / GBR / DEU)
#   - "azerbaij.pdf" — filename in Library of Congress ALA-LC romanization
#     URL anchors (citation source, not authored prose). Cited in
#     docs/decisions/0009-transliteration-profiles.md,
#     docs/features/transliteration.md, docs/open-questions.md
#
# Called by:
#   - The PreToolUse hook on Bash(git push *) in .claude/settings.json,
#     which blocks the push if this script exits non-zero
#   - Manually by contributors before any push to a public-or-soon-to-be-
#     public remote (see .claude/git-workflow.md)
#
# Exit codes:
#   0 — no private content found, OR no terms file present (no-op)
#   2 — private content found; offending lines printed to stderr; hook
#       interprets exit 2 as "block the tool call"
#
# Run from anywhere; the script resolves the project root automatically.

set -euo pipefail

PROJECT_ROOT="${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}"
cd "$PROJECT_ROOT"

TERMS_FILE=".claude/private-content-terms.local"

if [ ! -f "$TERMS_FILE" ]; then
    echo "private-content-scan: terms file not found at $TERMS_FILE; skipping."
    echo "  Each contributor maintains their own gitignored terms file."
    echo "  See .claude/git-workflow.md for setup."
    exit 0
fi

# Build a case-insensitive alternation pattern from the terms file.
# Strip blank lines and comment lines (leading #).
# Extended-regex (-E) — BRE \| is not portable (macOS BSD grep treats it literally).
PATTERN=$(grep -vE '^[[:space:]]*$|^[[:space:]]*#' "$TERMS_FILE" | paste -sd'|' -)

if [ -z "$PATTERN" ]; then
    echo "private-content-scan: terms file is empty; skipping."
    exit 0
fi

# Documented false positives. Lines matching ANY of these patterns are
# excluded from the result set. Extended-regex.
FALSE_POSITIVES='InvalidData|"Azerbaijan"|azerbaij\.pdf'

# Scan tracked files only (git ls-files = what a fresh cloner sees).
# -z for NUL-delimited paths handles unusual filenames.
HITS=$(
    git ls-files -z '*.md' '*.kt' '*.gradle*' \
        | xargs -0 grep -iEHn "$PATTERN" 2>/dev/null \
        || true
)

# Filter out documented false positives.
REAL_HITS=$(echo "$HITS" | grep -vE "$FALSE_POSITIVES" || true)

if [ -z "$REAL_HITS" ]; then
    echo "private-content-scan: no private content found in tracked files."
    exit 0
fi

{
    echo "private-content-scan: BLOCKED — possible private content found in tracked files."
    echo ""
    echo "$REAL_HITS"
    echo ""
    echo "Resolution:"
    echo "  1. Remove or rewrite the offending content, then re-run."
    echo "  2. OR if this is a new documented false positive (a generic"
    echo "     term that happens to substring-match), add it to"
    echo "     FALSE_POSITIVES in scripts/private-content-scan.sh AND"
    echo "     document it in .claude/git-workflow.md, then re-run."
} >&2

exit 2
