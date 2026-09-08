#!/usr/bin/env bash
set +x
set -euo pipefail
export SENTRY_LOAD_DOTENV=0
export SENTRY_DISABLE_UPDATE_CHECK=true

usage() {
  cat <<'USAGE'
Usage: scripts/sentry-upload-symbols.sh [--check] PATH

PATH is an .xcarchive, .dSYM bundle, or directory containing .dSYM bundles.
--check validates local DWARF files without contacting Sentry.

An upload requires sentry-cli and SENTRY_AUTH_TOKEN, SENTRY_ORG, SENTRY_PROJECT,
or an ignored sentry.properties file. SENTRY_PROPERTIES can select that file.
SENTRY_CLI can select an installed sentry-cli executable outside PATH.
Only debug symbols are uploaded; source files are not bundled.
USAGE
}

check_only=false
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
fi
if [[ "${1:-}" == "--check" ]]; then
  check_only=true
  shift
fi
if [[ $# -ne 1 || "$1" == -* ]]; then
  usage >&2
  exit 2
fi

sentry_cli="${SENTRY_CLI:-sentry-cli}"
if ! command -v "$sentry_cli" >/dev/null 2>&1; then
  printf '%s\n' 'sentry-cli is required. See https://docs.sentry.io/cli/installation/' >&2
  exit 1
fi

symbols_path="${1%/}"
if [[ "$symbols_path" == *.xcarchive ]]; then
  symbols_path="$symbols_path/dSYMs"
fi
if [[ ! -d "$symbols_path" ]]; then
  printf '%s\n' 'No debug-symbol directory exists at the supplied path.' >&2
  exit 1
fi
symbols_path="$(cd "$symbols_path" && pwd -P)"

found_symbols=false
symbol_files=()
while IFS= read -r -d '' dwarf_file; do
  found_symbols=true
  symbol_files+=("$dwarf_file")
  if [[ "$check_only" == true ]]; then
    "$sentry_cli" --log-level warn debug-files check "$dwarf_file"
  fi
done < <(find "$symbols_path" -type f -path '*.dSYM/Contents/Resources/DWARF/*' -print0)

if [[ "$found_symbols" != true ]]; then
  printf '%s\n' 'No dSYM DWARF files found. Archive the Release build with DWARF with dSYM File enabled.' >&2
  exit 1
fi
if [[ "$check_only" == true ]]; then
  exit 0
fi

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
if [[ -z "${SENTRY_PROPERTIES:-}" && -f "$repository_root/sentry.properties" ]]; then
  export SENTRY_PROPERTIES="$repository_root/sentry.properties"
fi
if [[ -n "${SENTRY_PROPERTIES:-}" ]]; then
  if [[ ! -f "$SENTRY_PROPERTIES" ]]; then
    printf '%s\n' 'SENTRY_PROPERTIES must name an existing properties file.' >&2
    exit 1
  fi
elif [[ -z "${SENTRY_AUTH_TOKEN:-}" || -z "${SENTRY_ORG:-}" || -z "${SENTRY_PROJECT:-}" ]]; then
  printf '%s\n' 'Set SENTRY_AUTH_TOKEN, SENTRY_ORG and SENTRY_PROJECT, or provide ignored sentry.properties.' >&2
  exit 1
fi

exec "$sentry_cli" --log-level warn debug-files upload --wait "${symbol_files[@]}"
