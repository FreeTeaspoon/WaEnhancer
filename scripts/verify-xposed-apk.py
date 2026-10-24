#!/usr/bin/env python3
"""Check the shipped libxposed entry metadata after packaging and shrinking."""

import argparse
import sys
import zipfile
from pathlib import Path


def verify(path: Path) -> None:
    prefix = "META-INF/xposed/"
    entry = "com.freeteaspoon.wppenhacer.WppXposed_YukiHookXposedInit"
    with zipfile.ZipFile(path) as apk:
        names = apk.namelist()
        for name in ("java_init.list", "module.prop", "scope.list"):
            if names.count(prefix + name) != 1:
                raise ValueError(f"missing or duplicate {prefix + name}")
        entries = apk.read(prefix + "java_init.list").decode().splitlines()
        if entries != [entry]:
            raise ValueError(f"unexpected module entry: {entries}")
        properties = dict(
            line.split("=", 1)
            for line in apk.read(prefix + "module.prop").decode().splitlines()
            if "=" in line and not line.startswith("#")
        )
        if properties.get("minApiVersion") != "101" or properties.get("targetApiVersion") != "101":
            raise ValueError(f"unexpected API requirements: {properties}")
        scope = set(apk.read(prefix + "scope.list").decode().splitlines())
        if scope != {"com.whatsapp", "com.whatsapp.w4b", "system"}:
            raise ValueError(f"unexpected scope: {scope}")
        if "assets/xposed_init" in names or "META-INF/yukihookapi_init" in names:
            raise ValueError("legacy Xposed entry is still packaged")
    print(f"{path}: libxposed API 101 metadata verified")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apks", nargs="+", type=Path)
    args = parser.parse_args()
    for path in args.apks:
        try:
            verify(path)
        except (OSError, ValueError, KeyError, zipfile.BadZipFile) as error:
            print(f"{path}: {error}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
