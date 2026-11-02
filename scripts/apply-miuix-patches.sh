#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
for patch_name in miuix-held-swipe.patch miuix-dialog-navigation.patch; do
    patch_file="$repo_root/third_party/patches/$patch_name"
    if git -C "$repo_root/third_party/miuix" apply --reverse --check "$patch_file" 2>/dev/null; then
        continue
    fi
    git -C "$repo_root/third_party/miuix" apply --check "$patch_file"
    git -C "$repo_root/third_party/miuix" apply "$patch_file"
done
