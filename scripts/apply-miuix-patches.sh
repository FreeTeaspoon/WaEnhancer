#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
patch_file="$repo_root/third_party/patches/miuix-held-swipe.patch"
if git -C "$repo_root/third_party/miuix" apply --reverse --check "$patch_file" 2>/dev/null; then
    exit 0
fi
git -C "$repo_root/third_party/miuix" apply --check "$patch_file"
git -C "$repo_root/third_party/miuix" apply "$patch_file"
