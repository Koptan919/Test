#!/usr/bin/env bash
# Validates a Noor content pack and writes the checksum file the app requires.
# Usage: scripts/prepare-content-pack.sh path/to/noor-content-pack.json
# Then attach BOTH files to a release tagged content-YYYY.MM.DD in the release repository.
set -euo pipefail
pack="${1:?usage: $0 noor-content-pack.json}"
[ "$(basename "$pack")" = "noor-content-pack.json" ] || { echo "File must be named noor-content-pack.json" >&2; exit 1; }
python3 - "$pack" <<'PY'
import json, re, sys
d = json.load(open(sys.argv[1], encoding="utf-8"))
assert d.get("format") == "noor-content-pack", "format must be noor-content-pack"
assert str(d.get("source", "")).strip(), "source is required"
items = d.get("items") or []
assert items, "items is empty"
for it in items:
    assert re.fullmatch(r"[a-z0-9_]{2,64}", it.get("id", "")), f"bad id: {it.get('id')}"
    assert [t for t in it.get("text", []) if str(t).strip()], f"{it['id']}: empty text"
print(f"OK: {len(items)} items from source: {d['source']}")
PY
sha256sum "$pack" | awk '{print $1"  noor-content-pack.json"}' > "$pack.sha256"
echo "Wrote $pack.sha256"
