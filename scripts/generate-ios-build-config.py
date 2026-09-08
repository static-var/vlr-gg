#!/usr/bin/env python3
"""Generate ignored Swift runtime settings without embedding Sentry upload credentials."""
from __future__ import annotations

import os
from pathlib import Path


def read_property(path: Path, key: str) -> str:
    if not path.exists():
        return ""
    for raw_line in path.read_text().splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        current_key, value = line.split("=", 1)
        if current_key.strip() == key:
            return normalize(value)
    return ""


def normalize(value: str | None) -> str:
    value = (value or "").strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in ('"', "'"):
        value = value[1:-1]
    return "" if value == "replace-with-your-token" else value


def setting(repo: Path, key: str, fallback: str = "") -> str:
    return (normalize(os.environ.get(key))
            or read_property(repo / "local.properties", key)
            or read_property(repo / ".env", key)
            or fallback)


def swift_string(value: str) -> str:
    # Swift interpolation must remain literal, including configuration containing \(.
    value = value.replace("\\", "\\\\").replace('"', '\\"')
    value = value.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    return '"' + value + '"'


def generate(repo: Path, destination: Path) -> None:
    token = (normalize(os.environ.get("VLR_AUTH_TOKEN"))
             or read_property(repo / "local.properties", "TOKEN")
             or setting(repo, "VLR_AUTH_TOKEN"))
    dsn = setting(repo, "SENTRY_DSN_IOS", setting(repo, "SENTRY_DSN"))
    environment = setting(repo, "SENTRY_ENVIRONMENT")
    enabled = setting(repo, "SENTRY_ENABLED", "true").lower() == "true"
    content = ("enum GeneratedBuildConfig {\n"
               f"    static let authToken: String? = {swift_string(token) if token else 'nil'}\n"
               f"    static let sentryDsn: String = {swift_string(dsn)}\n"
               f"    static let sentryEnvironment: String = {swift_string(environment)}\n"
               f"    static let sentryEnabled: Bool = {str(enabled).lower()}\n"
               "}\n")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.touch(mode=0o600, exist_ok=True)
    destination.chmod(0o600)
    destination.write_text(content)


if __name__ == "__main__":
    repo = Path(os.environ.get("REPO_ROOT", Path(__file__).resolve().parents[1]))
    target = Path(os.environ.get("GENERATED_SWIFT_PATH", repo / "iosApp/iosApp/GeneratedBuildConfig.swift"))
    generate(repo, target)
