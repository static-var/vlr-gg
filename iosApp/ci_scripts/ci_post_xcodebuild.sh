#!/bin/sh
set +x
set -eu

if [ "${CI_XCODEBUILD_ACTION:-}" != archive ]; then
  exit 0
fi

: "${CI_XCODEBUILD_EXIT_CODE:?Xcode Cloud must provide the archive exit code.}"
if [ "$CI_XCODEBUILD_EXIT_CODE" != 0 ]; then
  printf '%s\n' 'Archive failed; skipping Sentry debug-symbol upload.'
  exit 0
fi

: "${CI_ARCHIVE_PATH:?Xcode Cloud must provide the archive path.}"
: "${SENTRY_AUTH_TOKEN:?Set SENTRY_AUTH_TOKEN as an Xcode Cloud secret.}"
: "${SENTRY_ORG:?Set SENTRY_ORG in the Xcode Cloud workflow.}"
: "${SENTRY_PROJECT:?Set SENTRY_PROJECT in the Xcode Cloud workflow.}"

if ! command -v "${SENTRY_CLI:-sentry-cli}" >/dev/null 2>&1; then
  if [ -n "${SENTRY_CLI:-}" ]; then
    printf '%s\n' 'SENTRY_CLI must name an installed executable.' >&2
    exit 1
  fi
  brew install getsentry/tools/sentry-cli
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec /bin/bash "$SCRIPT_DIR/sentry-upload-symbols.sh" "$CI_ARCHIVE_PATH"
