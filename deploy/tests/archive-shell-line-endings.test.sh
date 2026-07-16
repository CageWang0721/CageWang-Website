#!/usr/bin/env bash
set -Eeuo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
archive_path="$(mktemp)"
trap 'rm -f "$archive_path"' EXIT

git -C "$root_dir" archive --worktree-attributes --format=zip --output="$archive_path" HEAD

python3 - "$archive_path" <<'PY'
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as archive:
    offenders = [
        name
        for name in archive.namelist()
        if name.startswith("deploy/scripts/")
        and name.endswith(".sh")
        and b"\r\n" in archive.read(name)
    ]

if offenders:
    raise SystemExit("CRLF shell scripts in deployment archive: " + ", ".join(offenders))
PY

echo "Deployment archive shell scripts use LF line endings."
