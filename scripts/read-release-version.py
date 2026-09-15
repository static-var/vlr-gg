#!/usr/bin/env python3
"""Print validated release versions in GitHub Actions environment format."""

from pathlib import Path
import re
import sys


def read_release_version(path: Path) -> tuple[str, str]:
    values = {}
    for number, raw_line in enumerate(path.read_text().splitlines(), start=1):
        line = raw_line.split("//", 1)[0].strip()
        if not line:
            continue
        match = re.fullmatch(r"(MARKETING_VERSION|CURRENT_PROJECT_VERSION)\s*=\s*(\S+)", line)
        if not match:
            raise ValueError(f"{path}:{number}: expected a release version assignment")
        key, value = match.groups()
        if key in values:
            raise ValueError(f"{path}:{number}: duplicate {key}")
        values[key] = value

    version_name = values.get("MARKETING_VERSION", "")
    version_code = values.get("CURRENT_PROJECT_VERSION", "")
    if not re.fullmatch(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)", version_name):
        raise ValueError("MARKETING_VERSION must contain three numeric components, such as 1.2.3")
    if not re.fullmatch(r"[1-9][0-9]{0,9}", version_code) or int(version_code) > 2_100_000_000:
        raise ValueError("CURRENT_PROJECT_VERSION must be an integer between 1 and 2100000000")
    return version_name, version_code


def main() -> int:
    path = Path(__file__).resolve().parent.parent / "version.xcconfig"
    try:
        version_name, version_code = read_release_version(path)
    except (OSError, ValueError) as error:
        print(f"Invalid release version: {error}", file=sys.stderr)
        return 1
    print(f"VERSION_NAME={version_name}\nVERSION_CODE={version_code}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
