#!/usr/bin/env python3
"""Render shipped SVG icon masters into lossless WebP settings previews."""

import io
from pathlib import Path
import subprocess

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ICONS = {
    "default": "match-point.svg",
    "amethyst": "alternates/glass-v.svg",
    "ticket": "alternates/match-ticket.svg",
    "arcade": "alternates/arcade.svg",
    "midnight": "alternates/midnight.svg",
    "mint": "alternates/mint.svg",
}
DESTINATION = ROOT / "feature-about/src/commonMain/composeResources/drawable"

for name, source in ICONS.items():
    rendered = subprocess.run(
        ["rsvg-convert", "-w", "216", "-h", "216", str(ROOT / "art/app-icon" / source)],
        check=True, capture_output=True,
    ).stdout
    with Image.open(io.BytesIO(rendered)) as image:
        image.save(DESTINATION / f"app_icon_preview_{name}.webp", lossless=True, method=6)
