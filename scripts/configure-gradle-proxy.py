#!/usr/bin/env python3
"""Pass Xcode Cloud's HTTP proxy settings to Gradle's JVM."""

import os
from pathlib import Path
from urllib.parse import unquote, urlsplit


def property_value(value: str) -> str:
    return (value.replace("\\", "\\\\")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("=", "\\=")
                 .replace(":", "\\:"))


properties = []
for scheme in ("http", "https"):
    raw = os.environ.get(f"{scheme.upper()}_PROXY") or os.environ.get(f"{scheme}_proxy")
    if not raw:
        continue
    proxy = urlsplit(raw if "://" in raw else f"http://{raw}")
    if not proxy.hostname:
        raise SystemExit(f"Invalid {scheme.upper()}_PROXY URL")
    properties.append(f"systemProp.{scheme}.proxyHost={property_value(proxy.hostname)}")
    properties.append(f"systemProp.{scheme}.proxyPort={proxy.port or 80}")
    if proxy.username:
        properties.append(f"systemProp.{scheme}.proxyUser={property_value(unquote(proxy.username))}")
    if proxy.password:
        properties.append(f"systemProp.{scheme}.proxyPassword={property_value(unquote(proxy.password))}")

if properties:
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    gradle_home.mkdir(parents=True, exist_ok=True)
    destination = gradle_home / "gradle.properties"
    with destination.open("a") as config:
        config.write("\n" + "\n".join(properties) + "\n")
    destination.chmod(0o600)
    print("Configured Gradle to use Xcode Cloud's proxy")
