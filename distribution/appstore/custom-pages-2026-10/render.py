#!/usr/bin/env python3
"""Render custom product page artwork with bundled brand fonts and real iOS captures."""

import argparse
import importlib.util
import json
from pathlib import Path

from fontTools.ttLib import TTFont
from PIL import Image, ImageDraw


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
RENDERER = ROOT / "distribution/rebrand-2026/template/render.py"
SPEC = importlib.util.spec_from_file_location("store_renderer", RENDERER)
renderer = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(renderer)


def text_bounds(slide, spec, brand):
    width, height = spec["width"], spec["height"]
    draw = ImageDraw.Draw(Image.new("RGB", (width, height)))
    margin = round(width * .075)
    size = round(width * (.105 if height / width < 2 else .112))
    face, lines = renderer.fit_headline(
        draw, slide["headline"], brand["fonts"]["headline"], width - margin * 2, 2, size
    )
    y = round(height * .095)
    bounds = []
    for line in lines:
        box = draw.textbbox((margin, y), line, font=face)
        assert margin <= box[0] and box[2] <= width - margin, (slide["headline"], box)
        assert box[3] < round(height * (.29 if height / width < 2 else .245)), box
        bounds.append(list(box))
        y += round(face.size * .89)
    kicker_face = renderer.font(brand["fonts"]["body_bold"], round(width * .023))
    meta_face = renderer.font(brand["fonts"]["body"], round(width * .018))
    kicker_width = sum(renderer.text_width(draw, letter, kicker_face) for letter in slide["kicker"])
    kicker_width += (len(slide["kicker"]) - 1) * width * .0024
    number_width = renderer.text_width(draw, f"{slide['id']} / 05", meta_face)
    assert margin + kicker_width + width * .025 < width - margin - number_width
    for key, value in (("headline", slide["headline"]), ("body", slide["kicker"])):
        with TTFont(brand["fonts"][key]) as font:
            missing = sorted({char for char in value if ord(char) not in font.getBestCmap()})
        assert not missing, (value, missing)
    return {"headline_size": face.size, "headline_lines": lines, "headline_bounds": bounds}


def render_page(name):
    manifest = HERE / "manifests" / f"{name}.json"
    data = renderer.load_manifest(manifest)
    brand = data["brand"]
    brand["fonts"] = {key: str(ROOT / path) for key, path in brand["fonts"].items()}
    output = HERE / "output" / name
    records, all_paths = [], []
    for target, spec in data["outputs"].items():
        frame = spec["device_frame"]
        frame["path"] = str(ROOT / frame["path"])
        frame["screen_mask"] = str(ROOT / frame["screen_mask"])
        folder = output / spec["directory"]
        folder.mkdir(parents=True, exist_ok=True)
        paths = []
        for index, slide in enumerate(data["slides"], 1):
            assert slide["id"] == f"{index:02}"
            source = ROOT / slide["sources"][spec["platform"]]
            text_check = text_bounds(slide, spec, brand)
            with Image.open(source) as capture:
                source_size = capture.size
                image, capture_size = renderer.render_slide(slide, spec, capture.copy(), False, brand)
            destination = folder / f"{slide['id']}.png"
            image.save(destination, optimize=True)
            with Image.open(destination) as actual:
                assert actual.format == "PNG" and actual.mode == "RGB"
                assert actual.size == (spec["width"], spec["height"])
            assert abs(source_size[0] / source_size[1] - capture_size[0] / capture_size[1]) < .002
            records.append({
                "asset": f"{target}/{slide['id']}", "path": str(destination),
                "dimensions": list(image.size), "source": str(source),
                "source_dimensions": list(source_size), "rendered_capture_dimensions": list(capture_size),
                "preview_source": False, "aspect_ratio_preserved": True, "text_validation": text_check,
            })
            paths.append(destination)
        renderer.make_contact_sheet(paths, output / f"contact-sheet-{target}.png", f"{name} / {target}", brand)
        all_paths.extend(paths)
    renderer.make_contact_sheet(all_paths, output / "contact-sheet-all.png", f"{name} / Val Esports", brand)
    report = {"manifest": str(manifest), "mode": "final", "asset_count": len(records),
              "fonts": brand["fonts"], "art_locale": data["art_locale"], "ui_locale": "en-US", "assets": records}
    (output / "validation.json").write_text(json.dumps(report, indent=2) + "\n")
    print(f"Rendered and validated {len(records)} assets: {output}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pages", nargs="*", choices=["matches", "rankings", "india", "brazil"])
    args = parser.parse_args()
    for page in args.pages or ["matches", "rankings", "india", "brazil"]:
        render_page(page)
