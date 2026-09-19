#!/usr/bin/env python3
"""Export the alternate SVG masters as adaptive, themed and legacy Android icons."""

from copy import deepcopy
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "androidApp/src/main/res"
SVG = "{http://www.w3.org/2000/svg}"
ANDROID = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID)
DENSITIES = (("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4))
ICONS = {"amethyst": "glass-v", "ticket": "match-ticket"}


def render(svg, pixels, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        ["rsvg-convert", "-w", str(pixels), "-h", str(pixels), "-o", str(destination)],
        input=ET.tostring(svg), check=True,
    )


def write_xml(root, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(root, space="    ")
    destination.write_text('<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(root, encoding="unicode") + "\n")


def monochrome(source, name):
    vector = ET.Element("vector", {
        f"{{{ANDROID}}}width": "108dp", f"{{{ANDROID}}}height": "108dp",
        f"{{{ANDROID}}}viewportWidth": "108", f"{{{ANDROID}}}viewportHeight": "108",
    })
    group = ET.SubElement(vector, "group", {
        f"{{{ANDROID}}}scaleX": "0.0703125", f"{{{ANDROID}}}scaleY": "0.0703125",
        f"{{{ANDROID}}}translateX": "18", f"{{{ANDROID}}}translateY": "18",
    })
    if name == "amethyst":
        silhouette = source.find(f"{SVG}defs/{SVG}clipPath[@id='outline']/{SVG}path").get("d")
    else:
        group = ET.SubElement(group, "group", {
            f"{{{ANDROID}}}rotation": "-10", f"{{{ANDROID}}}pivotX": "512", f"{{{ANDROID}}}pivotY": "512",
        })
        foreground = source.find(f"{SVG}g[@id='foreground']")
        silhouette = foreground.find(f"{SVG}path[@fill='url(#paper)']").get("d")
        silhouette += " " + foreground.find(f"{SVG}path[@fill='url(#violet)']").get("d")
        # Cut the three stub marks out of the opaque paper silhouette.
        silhouette += " " + foreground.findall(f"{SVG}path[@fill='url(#violet)']")[1].get("d")
    ET.SubElement(group, "path", {
        f"{{{ANDROID}}}fillColor": "#FFFFFFFF", f"{{{ANDROID}}}fillType": "evenOdd",
        f"{{{ANDROID}}}pathData": silhouette,
    })
    write_xml(vector, RES / f"drawable/ic_launcher_{name}_monochrome.xml")


def main():
    for name, master in ICONS.items():
        source = ET.parse(ROOT / f"art/app-icon/alternates/{master}.svg").getroot()
        foreground = deepcopy(source)
        foreground.remove(foreground.find(f"{SVG}g[@id='background']"))
        # Map the square master into the central 72 dp of the 108 dp adaptive canvas.
        foreground.set("viewBox", "-256 -256 1536 1536")
        background = deepcopy(source)
        background.remove(background.find(f"{SVG}g[@id='foreground']"))
        monochrome(source, name)
        adaptive = ET.Element("adaptive-icon")
        for layer in ("background", "foreground", "monochrome"):
            ET.SubElement(adaptive, layer, {f"{{{ANDROID}}}drawable": f"@drawable/ic_launcher_{name}_{layer}"})
        for suffix in ("", "_round"):
            write_xml(adaptive, RES / f"mipmap-anydpi-v26/ic_launcher_{name}{suffix}.xml")
        for density, scale in DENSITIES:
            for layer, svg in (("background", background), ("foreground", foreground)):
                render(svg, int(108 * scale), RES / f"drawable-{density}/ic_launcher_{name}_{layer}.png")
            for suffix, mask in (("", {"width": "1024", "height": "1024", "rx": "236"}), ("_round", {"cx": "512", "cy": "512", "r": "512"})):
                legacy = deepcopy(source)
                clip = ET.SubElement(legacy.find(f"{SVG}defs"), f"{SVG}clipPath", {"id": "legacy-mask"})
                ET.SubElement(clip, f"{SVG}circle" if suffix else f"{SVG}rect", mask)
                group = ET.Element(f"{SVG}g", {"clip-path": "url(#legacy-mask)"})
                for layer in ("background", "foreground"):
                    node = legacy.find(f"{SVG}g[@id='{layer}']")
                    legacy.remove(node)
                    group.append(node)
                legacy.append(group)
                render(legacy, int(48 * scale), RES / f"mipmap-{density}/ic_launcher_{name}{suffix}.png")
        print(f"Generated Android adaptive, monochrome and legacy assets: {name}")


if __name__ == "__main__":
    main()
