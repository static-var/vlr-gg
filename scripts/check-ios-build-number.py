#!/usr/bin/env python3
"""Stop an Xcode Cloud release whose build number differs from version.xcconfig.

Xcode Cloud replaces CFBundleVersion with its own build counter, while the Android
release workflow reads CURRENT_PROJECT_VERSION. Releasing only when the two match keeps
both stores on the same build number.
"""

from __future__ import annotations

import importlib.util
import os
from pathlib import Path
import sys

# Xcode Cloud workflows that distribute builds to testers or the App Store.
RELEASE_WORKFLOWS = {"iOS TestFlight Release"}

ROOT = Path(__file__).resolve().parent.parent
_SPEC = importlib.util.spec_from_file_location("read_release_version", ROOT / "scripts" / "read-release-version.py")
_release_version = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(_release_version)


def check(environ: dict[str, str], version_file: Path) -> str | None:
    """Return an error message, or None when the build may continue."""
    workflow = environ.get("CI_WORKFLOW", "")
    if workflow not in RELEASE_WORKFLOWS:
        return None
    build_number = environ.get("CI_BUILD_NUMBER", "")
    version_name, version_code = _release_version.read_release_version(version_file)
    if build_number == version_code:
        return None
    return (
        f"Xcode Cloud build {build_number or '(unset)'} does not match CURRENT_PROJECT_VERSION "
        f"{version_code} in version.xcconfig for {version_name}. Android and iOS must share build "
        "numbers: set CURRENT_PROJECT_VERSION to Xcode Cloud's next build number, or raise the next "
        "build number in Xcode Cloud settings to match, then start the build again."
    )


def main() -> int:
    try:
        error = check(dict(os.environ), ROOT / "version.xcconfig")
    except (OSError, ValueError) as exception:
        error = f"Invalid release version: {exception}"
    if error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
