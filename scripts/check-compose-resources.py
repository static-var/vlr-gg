#!/usr/bin/env python3
"""Reject Android artifacts missing files referenced by generated Compose accessors."""

import argparse
from pathlib import Path
import re
import sys
from zipfile import BadZipFile, ZipFile


def expected_resources(root: Path) -> set[str]:
    expected = set()
    for source in root.glob("*/build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors/**/*.kt"):
        text = source.read_text()
        item_count = text.count("ResourceItem(")
        if not item_count:
            continue
        directory = re.search(r'const val MD: String = "(composeResources/[^"\n]+)"', text)
        paths = re.findall(r'"(\$\{MD\}[^"\n]+|composeResources/[^"\n]+)"', text)
        paths = [path for path in paths if not directory or path != directory.group(1)]
        if len(paths) != item_count or (any("${MD}" in path for path in paths) and not directory):
            raise ValueError(f"Cannot read resource paths from {source}")
        expected.update(path.replace("${MD}", directory.group(1) if directory else "") for path in paths)
    if not expected:
        raise ValueError("No generated Compose resource references found; build the Android artifact first")
    return expected


def check_artifact(root: Path, artifact: Path) -> int:
    expected = expected_resources(root)
    prefixes = {".apk": "assets/", ".aab": "base/assets/"}
    if artifact.suffix not in prefixes:
        raise ValueError("Expected an .apk or .aab artifact")
    with ZipFile(artifact) as archive:
        entries = set(archive.namelist())
    missing = sorted(path for path in expected if prefixes[artifact.suffix] + path not in entries)
    if missing:
        raise ValueError("Missing Compose resources:\n" + "\n".join(missing))
    return len(expected)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("artifact", type=Path)
    args = parser.parse_args()
    try:
        count = check_artifact(Path(__file__).resolve().parent.parent, args.artifact)
    except (OSError, ValueError, BadZipFile) as error:
        print(error, file=sys.stderr)
        return 1
    print(f"Verified {count} Compose resource files in {args.artifact}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
