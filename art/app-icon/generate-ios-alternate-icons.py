#!/usr/bin/env python3
"""Generate layered iOS icons from the alternate SVG masters."""

import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
SVG = "{http://www.w3.org/2000/svg}"
ET.register_namespace("", "http://www.w3.org/2000/svg")
ALTERNATES = {
    "AppIconArcade": ("arcade.svg", "0.22,0.14,0.40,1", "0.067,0.039,0.125,1"),
    "AppIconMidnight": ("midnight.svg", "0.15,0.16,0.25,1", "0.031,0.035,0.063,1"),
    "AppIconMint": ("mint.svg", "0.76,0.92,0.85,1", "0.039,0.118,0.094,1"),
    "AppIconAmethyst": ("glass-v.svg", "0.90,0.86,0.96,1", "0.047,0.051,0.086,1"),
    "AppIconTicket": ("match-ticket.svg", "0.02,0.22,0.80,1", "0.031,0.059,0.125,1"),
}


def generate_icon(name, source_name, color, dark_color):
    svg = ET.parse(ROOT / "art/app-icon/alternates" / source_name).getroot()
    svg.remove(svg.find(f"{SVG}g[@id='background']"))
    foreground = svg.find(f"{SVG}g[@id='foreground']")
    for shadow in foreground.findall(f"{SVG}ellipse"):
        foreground.remove(shadow)
    destination = ROOT / "iosApp/iosApp" / f"{name}.icon"
    assets = destination / "Assets"
    assets.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(svg).write(assets / "foreground.svg", encoding="unicode")
    contents = {
        "fill-specializations": [
            {"value": {"solid": f"extended-srgb:{color}"}},
            {"appearance": "dark", "value": {"solid": f"extended-srgb:{dark_color}"}},
        ],
        "groups": [{"name": "Artwork", "layers": [
            {"name": "Foreground", "image-name": "foreground.svg"},
        ]}],
        "supported-platforms": {"squares": ["iOS"]},
    }
    (destination / "icon.json").write_text(json.dumps(contents, indent=2) + "\n")
    print(f"Generated {name}: layered iOS icon.")


if __name__ == "__main__":
    for name, (source, color, dark_color) in ALTERNATES.items():
        generate_icon(name, source, color, dark_color)
