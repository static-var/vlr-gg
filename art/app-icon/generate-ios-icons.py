#!/usr/bin/env python3
"""Render the selected Match point SVG into iOS app icon assets.

Requires Python 3, Pillow, and rsvg-convert (librsvg).
Run from any directory: python3 art/app-icon/generate-ios-icons.py
"""

import io
import json
from pathlib import Path
import subprocess

from PIL import Image


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/app-icon/match-point.svg"
DESTINATION = ROOT / "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
SLOTS = {
    "iphone": [(20, 2), (20, 3), (29, 1), (29, 2), (29, 3), (40, 2), (40, 3), (60, 2), (60, 3)],
    "ipad": [(20, 1), (20, 2), (29, 1), (29, 2), (40, 1), (40, 2), (76, 1), (76, 2), (83.5, 2)],
    "ios-marketing": [(1024, 1)],
}


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


if __name__ == "__main__":
    main()
