#!/usr/bin/env python3
"""Generate Android launcher resources from the selected Match point SVG."""

from pathlib import Path
import subprocess
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/app-icon/match-point.svg"
RES = ROOT / "androidApp/src/main/res"
SVG = "{http://www.w3.org/2000/svg}"
ANDROID = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID)


def vector(source: Path, output: Path, *, splash: bool = False) -> None:
    svg = ET.parse(source).getroot()
    foreground = svg.find(f"{SVG}g[@id='foreground']")
    assert foreground is not None
    root = ET.Element("vector", {
        f"{{{ANDROID}}}width": "288dp" if splash else "108dp",
        f"{{{ANDROID}}}height": "288dp" if splash else "108dp",
        f"{{{ANDROID}}}viewportWidth": "96" if splash else "108",
        f"{{{ANDROID}}}viewportHeight": "96" if splash else "108",
    })
    # The 64-unit master maps to Android's central 72 dp mask area.
    # The splash centers the painted bounds on a 288 dp canvas.
    group = ET.SubElement(root, "group", {
        f"{{{ANDROID}}}scaleX": "1" if splash else "1.125",
        f"{{{ANDROID}}}scaleY": "1" if splash else "1.125",
        f"{{{ANDROID}}}translateX": "16" if splash else "18",
        f"{{{ANDROID}}}translateY": "15" if splash else "18",
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
                subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), str(svg), "-o", str(RES / f"mipmap-{density}/{name}.png")], check=True)
    print("Generated Android adaptive, themed, legacy, and splash Match point assets.")


if __name__ == "__main__":
    main()
