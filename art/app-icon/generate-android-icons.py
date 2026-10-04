#!/usr/bin/env python3
"""Generate Android launcher resources from the selected Match point SVG."""

from pathlib import Path
from io import BytesIO
import subprocess
import tempfile
import xml.etree.ElementTree as ET

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/app-icon/match-point.svg"
RES = ROOT / "androidApp/src/main/res"
SVG = "{http://www.w3.org/2000/svg}"
ANDROID = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID)


def vector(source: Path, output: Path, *, splash: bool = False, notification: bool = False) -> None:
    svg = ET.parse(source).getroot()
    foreground = svg.find(f"{SVG}g[@id='foreground']")
    assert foreground is not None
    if notification:
        size, viewport, scale, translate_x, translate_y = "24dp", "24", "0.5", "-4", "-4"
    elif splash:
        size, viewport, scale, translate_x, translate_y = "288dp", "96", "1", "16", "15"
    else:
        size, viewport, scale, translate_x, translate_y = "108dp", "108", "1.125", "18", "18"
    root = ET.Element("vector", {
        f"{{{ANDROID}}}width": size,
        f"{{{ANDROID}}}height": size,
        f"{{{ANDROID}}}viewportWidth": viewport,
        f"{{{ANDROID}}}viewportHeight": viewport,
    })
    group = ET.SubElement(root, "group", {
        f"{{{ANDROID}}}scaleX": scale,
        f"{{{ANDROID}}}scaleY": scale,
        f"{{{ANDROID}}}translateX": translate_x,
        f"{{{ANDROID}}}translateY": translate_y,
    })
    for path in foreground:
        assert path.tag == f"{SVG}path", "Expected flat SVG paths"
        attrs = {
            f"{{{ANDROID}}}fillColor": path.attrib["fill"],
            f"{{{ANDROID}}}pathData": path.attrib["d"],
        }
        if path.get("fill-rule") == "evenodd":
            attrs[f"{{{ANDROID}}}fillType"] = "evenOdd"
        ET.SubElement(group, "path", attrs)
    ET.indent(root, space="    ")
    output.write_text('<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(root, encoding="unicode") + "\n")


def main() -> None:
    vector(SOURCE, RES / "drawable/ic_launcher_foreground.xml")
    vector(SOURCE, RES / "drawable/ic_splash_logo.xml", splash=True)
    vector(Path(__file__).with_name("match-point-monochrome.svg"), RES / "drawable/ic_launcher_monochrome.xml")
    vector(Path(__file__).with_name("match-point-monochrome.svg"), RES / "drawable/ic_notification.xml", notification=True)
    source = ET.parse(SOURCE).getroot()
    background = source.find(f"{SVG}g[@id='background']/{SVG}path").attrib["fill"]
    colors_file = RES / "values/colors.xml"
    colors = ET.parse(colors_file).getroot()
    colors.find("color[@name='ic_launcher_background']").text = background
    ET.indent(colors, space="    ")
    colors_file.write_text('<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(colors, encoding="unicode") + "\n")
    adaptive = f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="{ANDROID}">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
</adaptive-icon>
'''
    for name in ("ic_launcher", "ic_launcher_round"):
        (RES / f"mipmap-anydpi-v26/{name}.xml").write_text(adaptive)
    raw = SOURCE.read_text()
    inner = raw[raw.index(">") + 1:raw.rindex("</svg>")]
    with tempfile.TemporaryDirectory() as temporary:
        for name, mask in (
            ("ic_launcher", '<rect width="64" height="64" rx="14.72"/>'),
            ("ic_launcher_round", '<circle cx="32" cy="32" r="32"/>'),
        ):
            svg = Path(temporary) / f"{name}.svg"
            svg.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none"><defs><clipPath id="launcher-mask">{mask}</clipPath></defs><g clip-path="url(#launcher-mask)">{inner}</g></svg>')
            for density, size in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
                rendered = subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), str(svg)], check=True, capture_output=True)
                with Image.open(BytesIO(rendered.stdout)) as image:
                    image.save(RES / f"mipmap-{density}/{name}.webp", "WEBP", lossless=True, method=6)
    print("Generated Android adaptive, themed, legacy, and splash Match point assets.")


if __name__ == "__main__":
    main()
