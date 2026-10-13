#!/bin/sh
set -eu

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
export REPO_ROOT

# Release builds must use the build number in version.xcconfig, which Android also ships.
python3 "$REPO_ROOT/scripts/check-ios-build-number.py"

for swift_path in "$REPO_ROOT/iosApp/iosApp/GeneratedBuildConfig.swift" "$REPO_ROOT/iosApp/VLRWidget/GeneratedBuildConfig.swift"; do
  GENERATED_SWIFT_PATH="$swift_path" python3 "$REPO_ROOT/scripts/generate-ios-build-config.py"
done

python3 "$REPO_ROOT/scripts/configure-gradle-proxy.py"

brew install openjdk@17
