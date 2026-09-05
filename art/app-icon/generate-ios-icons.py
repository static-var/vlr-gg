#!/usr/bin/env python3
"""Render the selected Match point SVG into iOS app icon and launch assets.

Requires Python 3, Pillow, and rsvg-convert (librsvg).
Run from any directory: python3 art/app-icon/generate-ios-icons.py
"""

import io
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/app-icon/match-point.svg"
DESTINATION = ROOT / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
LAUNCH_DESTINATION = DESTINATION.parent / "LaunchLogo.imageset"
SLOTS = {
    "iphone": [(20, 2), (20, 3), (29, 1), (29, 2), (29, 3), (40, 2), (40, 3), (60, 2), (60, 3)],
    "ipad": [(20, 1), (20, 2), (29, 1), (29, 2), (40, 1), (40, 2), (76, 1), (76, 2), (83.5, 2)],
    "ios-marketing": [(1024, 1)],
}


def generate_launch_logo():
    svg = ET.parse(SOURCE).getroot()
    background = next(child for child in svg if child.get("id") == "background")
    svg.remove(background)
    # The foreground spans x=10..54, y=12..54, centered at (32, 33).
    svg.set("viewBox", "0 1 64 64")
    source = ET.tostring(svg)
    LAUNCH_DESTINATION.mkdir(parents=True, exist_ok=True)
    images = []
    for scale in (1, 2, 3):
        pixels = 192 * scale
        filename = f"launch-logo@{scale}x.png"
        png = subprocess.run(
            ["rsvg-convert", "--width", str(pixels), "--height", str(pixels)],
            input=source,
            check=True,
            capture_output=True,
        ).stdout
        with Image.open(io.BytesIO(png)) as image:
            assert image.size == (pixels, pixels), filename
            image.convert("RGBA").save(LAUNCH_DESTINATION / filename, optimize=True)
        images.append({"filename": filename, "idiom": "universal", "scale": f"{scale}x"})
    contents = {"images": images, "info": {"author": "xcode", "version": 1}}
    (LAUNCH_DESTINATION / "Contents.json").write_text(json.dumps(contents, indent=2) + "\n")
    print("Generated centered, transparent launch logo at 1x, 2x, and 3x.")


def main():
    DESTINATION.mkdir(parents=True, exist_ok=True)
    images = []
    rendered = set()
    for idiom, slots in SLOTS.items():
        for points, scale in slots:
            suffix = f"@{scale}x" if scale != 1 else ""
            filename = f"icon-{points}{suffix}.png"
            pixels = int(points * scale)
            if filename not in rendered:
                png = subprocess.run(
                    ["rsvg-convert", "--width", str(pixels), "--height", str(pixels), str(SOURCE)],
                    check=True,
                    capture_output=True,
                ).stdout
                with Image.open(io.BytesIO(png)) as image:
                    assert image.size == (pixels, pixels), filename
                    assert image.convert("RGBA").getchannel("A").getextrema() == (255, 255), filename
                    image.convert("RGB").save(DESTINATION / filename, optimize=True)
                rendered.add(filename)
            images.append({
                "filename": filename,
                "idiom": idiom,
                "scale": f"{scale}x",
                "size": f"{points}x{points}",
            })
    contents = {"images": images, "info": {"author": "xcode", "version": 1}}
    (DESTINATION / "Contents.json").write_text(json.dumps(contents, indent=2) + "\n")
    print(f"Generated {len(rendered)} opaque icons for {len(images)} iOS asset slots.")
    generate_launch_logo()


if __name__ == "__main__":
    main()
