#!/usr/bin/env bash
# Verify every backtick-wrapped `*.md` filename mentioned in committed docs
# refers to a tracked file in the repo.
#
# Complements scripts/check-markdown-links.sh, which checks clickable
# markdown links. This script catches prose mentions like
#   "see `mrz-data-model.md`"
# that can rot silently (rename, deletion) without a clickable link.
#
# Ground truth on both ends is `git ls-files`:
# - Input: only tracked .md files are grepped, so prose inside gitignored
#   working notes (SESSION-HANDOFF-*, etc.) does not leak into the input.
# - Existence: tracked basenames, so the check matches what a fresh cloner
#   sees, not the local working tree.
#
# Filenames matching the patterns in is_external_reference() are allowed
# even though they are not tracked, because prose mentions of those
# artifacts are intentional references to things that live outside the repo.
#
# Limitations:
# - Basename-only check; if `foo.md` is mentioned and a tracked foo.md exists
#   anywhere in the tree, the reference passes regardless of intended path.
#   The companion check-markdown-links.sh handles path correctness for
#   clickable links.
# - Only catches backtick-wrapped bare filenames (no slashes), matching the
#   common prose-mention convention in this project.
#
# Exit code: number of missing references (0 = all resolved).
#
# Run from project root.

set -euo pipefail

tracked_basenames=$(git ls-files | awk -F/ '{print $NF}' | LC_ALL=C sort -u)

is_external_reference() {
    # Filenames intentionally not tracked by this repo but referenced in docs.
    case "$1" in
        # Local working notes — gitignored by design (see .gitignore).
        SESSION-HANDOFF-*.md|RECAP-*.md|CONFORMANCE-*.md) return 0 ;;
        # Auto-memory files in ~/.claude/projects/<...>/memory/ — referenced
        # in docs that explain the memory convention. Naming follows the
        # convention documented in CLAUDE.md / .claude/working-patterns.md.
        MEMORY.md|feedback_*.md|reference_*.md|user_*.md|project_*.md) return 0 ;;
        *) return 1 ;;
    esac
}

missing=0
while IFS= read -r ref; do
    if is_external_reference "$ref"; then
        continue
    fi
    if ! printf '%s\n' "$tracked_basenames" | grep -Fxq -- "$ref"; then
        echo "MISSING: $ref"
        missing=$((missing + 1))
    fi
done < <(
    git ls-files -z '*.md' \
        | xargs -0 grep -oh '`[A-Za-z0-9][A-Za-z0-9._-]*\.md`' 2>/dev/null \
        | tr -d '`' | LC_ALL=C sort -u
)

if [ "$missing" -eq 0 ]; then
    echo "All cross-references resolve."
fi

exit "$missing"
