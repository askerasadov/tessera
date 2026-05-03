#!/usr/bin/env bash
# Verify every backtick-wrapped .md cross-reference in the documentation
# resolves to an existing file in the repo.
#
# Exit code: number of missing references (0 = all resolved).
#
# Run from project root.

set -euo pipefail

missing=0
while IFS= read -r ref; do
    hits=$(find . -name "$ref" \
        -not -path "./.git/*" \
        -not -path "./build/*" \
        -not -path "./*/build/*" \
        -not -path "./.gradle/*" \
        | wc -l | tr -d ' ')
    if [ "$hits" -eq 0 ]; then
        echo "MISSING: $ref"
        missing=$((missing + 1))
    fi
done < <(grep -roh '`[a-z][a-z0-9-]*\.md`' --include="*.md" . | tr -d '`' | sort -u)

if [ "$missing" -eq 0 ]; then
    echo "All cross-references resolve."
fi

exit "$missing"
