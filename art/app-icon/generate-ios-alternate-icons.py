#!/usr/bin/env python3
"""Render alternate iOS app icon sets with Python 3, Pillow, and rsvg-convert.

Run from any directory: python3 art/app-icon/generate-ios-alternate-icons.py
"""

import io
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SVG = "{http://www.w3.org/2000/svg}"
ALTERNATES = {
    "AppIconAmethyst": "glass-v.svg",
    "AppIconTicket": "match-ticket.svg",
}


def generate_icon_set(name, source_name):
    source = ROOT / "art/app-icon/alternates" / source_name
    destination = ROOT / "iosApp/iosApp/Assets.xcassets" / f"{name}.appiconset"
    destination.mkdir(parents=True, exist_ok=True)
    images = []
    for appearance in ("light", "dark"):
        svg = ET.parse(source).getroot()
        if name == "AppIconAmethyst" and appearance == "light":
            stops = svg.findall(f"{SVG}defs/{SVG}radialGradient[@id='backdrop']/{SVG}stop")
            for stop, color in zip(stops, ("#F7F3FF", "#DBD3EE"), strict=True):
                stop.set("stop-color", color)
        elif name == "AppIconTicket" and appearance == "dark":
            stops = svg.findall(f"{SVG}defs/{SVG}linearGradient[@id='cobalt']/{SVG}stop")
            for stop, color in zip(stops, ("#16233F", "#080F20"), strict=True):
                stop.set("stop-color", color)
        filename = f"icon-1024-{appearance}.png"
        png = subprocess.run(
            ["rsvg-convert", "--width", "1024", "--height", "1024"],
            input=ET.tostring(svg), check=True, capture_output=True,
        ).stdout
        with Image.open(io.BytesIO(png)) as image:
            assert image.size == (1024, 1024), filename
            assert image.convert("RGBA").getchannel("A").getextrema() == (255, 255), filename
            image.convert("RGB").save(destination / filename, optimize=True)
        slot = {"filename": filename, "idiom": "universal", "platform": "ios", "size": "1024x1024"}
        if appearance == "dark":
            slot["appearances"] = [{"appearance": "luminosity", "value": "dark"}]
        images.append(slot)
    # Xcode generates device sizes from the universal masters.
    for previous in destination.glob("icon-*.png"):
        if previous.name not in {image["filename"] for image in images}:
            previous.unlink()
    contents = {"images": images, "info": {"author": "xcode", "version": 1}}
    (destination / "Contents.json").write_text(json.dumps(contents, indent=2) + "\n")
    print(f"Generated {name}: light and dark universal iOS icons.")


def main():
    for name, source_name in ALTERNATES.items():
        generate_icon_set(name, source_name)


if __name__ == "__main__":
    main()
