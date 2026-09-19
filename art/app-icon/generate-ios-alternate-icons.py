#!/usr/bin/env python3
"""Render alternate iOS app icon sets with Python 3, Pillow, and rsvg-convert.

Run from any directory: python3 art/app-icon/generate-ios-alternate-icons.py
"""

import io
import json
from pathlib import Path
import runpy
import subprocess

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SLOTS = runpy.run_path(str(ROOT / "art/app-icon/generate-ios-icons.py"))["SLOTS"]
ALTERNATES = {
    "AppIconAmethyst": "glass-v.svg",
    "AppIconTicket": "match-ticket.svg",
}


def generate_icon_set(name, source_name):
    source = ROOT / "art/app-icon/alternates" / source_name
    destination = ROOT / "iosApp/iosApp/Assets.xcassets" / f"{name}.appiconset"
    destination.mkdir(parents=True, exist_ok=True)
    images = []
    rendered = set()
    for idiom, slots in SLOTS.items():
        for points, scale in slots:
            suffix = f"@{scale}x" if scale != 1 else ""
            filename = f"icon-{points}{suffix}.png"
            pixels = int(points * scale)
            if filename not in rendered:
                png = subprocess.run(
                    ["rsvg-convert", "--width", str(pixels), "--height", str(pixels), str(source)],
                    check=True,
                    capture_output=True,
                ).stdout
                with Image.open(io.BytesIO(png)) as image:
                    assert image.size == (pixels, pixels), filename
                    assert image.convert("RGBA").getchannel("A").getextrema() == (255, 255), filename
                    image.convert("RGB").save(destination / filename, optimize=True)
                rendered.add(filename)
            images.append({
                "filename": filename,
                "idiom": idiom,
                "scale": f"{scale}x",
                "size": f"{points}x{points}",
            })
    contents = {"images": images, "info": {"author": "xcode", "version": 1}}
    (destination / "Contents.json").write_text(json.dumps(contents, indent=2) + "\n")
    print(f"Generated {name}: {len(rendered)} opaque icons for {len(images)} iOS asset slots.")


def main():
    for name, source_name in ALTERNATES.items():
        generate_icon_set(name, source_name)


if __name__ == "__main__":
    main()
