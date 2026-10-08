"""Render App Store creative assets. Requires Pillow, fontTools and rsvg-convert."""

import base64
import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
INK, IVORY, VIOLET, LILAC = "#16131B", "#F5F1E8", "#956DFF", "#D9CCFF"
HEADLINE = ROOT / "designsystem/src/commonMain/composeResources/font/chakra_petch_regular.ttf"
BODY = ROOT / "designsystem/src/commonMain/composeResources/font/space_grotesk_regular.ttf"
CAPTURE = ROOT / "distribution/rebrand-2026/raw/ios/02.png"
ICON = ROOT / "art/app-icon/match-point.svg"
NS = "{http://www.w3.org/2000/svg}"
SPECS = {
    "header": {"size": (3840, 1646), "safe": (1097, 493, 2743, 1154)},
    "search": {"size": (3840, 2560), "safe": (836, 765, 3004, 1795)},
}


def text_path(label, font_path, size, x, baseline, fill, boxes):
    font = TTFont(font_path)
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    scale = size / font["head"].unitsPerEm
    stroke = size * 0.016 if font_path == HEADLINE else 0
    paths = []
    for character in label:
        glyph = glyphs[cmap[ord(character)]]
        transform = (scale, 0, 0, -scale, x, baseline)
        pen = SVGPathPen(glyphs)
        glyph.draw(TransformPen(pen, transform))
        paths.append(pen.getCommands())
        bounds = BoundsPen(glyphs)
        glyph.draw(TransformPen(bounds, transform))
        if bounds.bounds:
            left, top, right, bottom = bounds.bounds
            boxes.append((label, (left-stroke/2, top-stroke/2, right+stroke/2, bottom+stroke/2)))
        x += glyph.width * scale
    font.close()
    return f'<path fill="{fill}" stroke="{fill}" stroke-width="{stroke}" paint-order="stroke" d="{" ".join(paths)}"/>'


def icon(x, y, size):
    tree = ET.parse(ICON)
    groups = "".join(ET.tostring(group, encoding="unicode") for group in tree.getroot().findall(NS + "g"))
    return f'<svg x="{x}" y="{y}" width="{size}" height="{size}" viewBox="0 0 64 64">{groups}</svg>'


def screenshot(x, y, width, crop=None):
    with Image.open(CAPTURE) as source:
        sw, sh = source.size
    payload = base64.b64encode(CAPTURE.read_bytes()).decode()
    left, top, right, bottom = crop or (0, 0, sw, sh)
    height = width * (bottom - top) / (right - left)
    return (
        f'<svg x="{x}" y="{y}" width="{width}" height="{height}" '
        f'viewBox="{left} {top} {right-left} {bottom-top}" overflow="hidden">'
        f'<image width="{sw}" height="{sh}" href="data:image/png;base64,{payload}"/></svg>',
        (x, y, x + width, y + height),
    )


def background(width, height):
    lines = "".join(
        f'<path d="M{x} 0L{x-height} {height}"/>' for x in range(-height, width + height, 256)
    )
    return f'''<defs>
      <radialGradient id="glow"><stop stop-color="#493660"/><stop offset="1" stop-color="{INK}"/></radialGradient>
    </defs>
    <rect width="{width}" height="{height}" fill="{INK}"/>
    <ellipse cx="{width/2}" cy="{height/2}" rx="{width*.58}" ry="{height*.8}" fill="url(#glow)"/>
    <g stroke="{VIOLET}" stroke-width="2" opacity=".09">{lines}</g>
    <g stroke="{VIOLET}" stroke-width="6" fill="none" opacity=".32">
      <path d="M230 340H680V540H950M230 740H680V540M230 {height-340}H680V{height-540}H950M230 {height-740}H680V{height-540}"/>
    </g>'''


def compose(kind):
    spec = SPECS[kind]
    width, height = spec["size"]
    boxes = []
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             background(width, height)]
    if kind == "header":
        parts.append(icon(1137, 623, 440))
        boxes.append(("shipping icon", (1137, 623, 1577, 1063)))
        for label, font, size, x, baseline, fill in [
            ("VAL ESPORTS", BODY, 48, 1680, 624, LILAC),
            ("Every match.", HEADLINE, 150, 1670, 805, IVORY),
            ("Your way.", HEADLINE, 150, 1670, 954, IVORY),
            ("Scores. Schedules. Favorites.", BODY, 43, 1680, 1085, LILAC),
        ]:
            parts.append(text_path(label, font, size, x, baseline, fill, boxes))
        # Peripheral UI is decorative; the message remains inside Apple's art safe area.
        ui, _ = screenshot(3050, 160, 610)
        parts += ['<g transform="rotate(8 3355 823)">',
                  '<rect x="3028" y="138" width="654" height="1370" rx="32" fill="#08070A"/>', ui, '</g>']
    else:
        parts.append(icon(940, 836, 112))
        boxes.append(("shipping icon", (940, 836, 1052, 948)))
        for label, font, size, x, baseline, fill in [
            ("VAL ESPORTS", BODY, 50, 1090, 907, LILAC),
            ("VALORANT", HEADLINE, 150, 940, 1155, IVORY),
            ("scores & stats", HEADLINE, 130, 940, 1310, IVORY),
            ("Follow your favorites.", BODY, 48, 944, 1486, LILAC),
            ("Schedules. Results. Events.", BODY, 43, 944, 1564, LILAC),
        ]:
            parts.append(text_path(label, font, size, x, baseline, fill, boxes))
        ui, bounds = screenshot(2140, 912, 790, (49, 474, 1157, 1408))
        parts += [f'<rect x="2120" y="892" width="830" height="706" fill="{VIOLET}"/>', ui]
        boxes.append(("actual match score UI", bounds))
        parts.append(f'<path d="M940 1670H1150" stroke="{VIOLET}" stroke-width="12"/>')
    parts.append('</svg>')
    return "".join(parts), boxes


def main():
    output = HERE / "output"
    output.mkdir(exist_ok=True)
    records = []
    preview = Image.new("RGB", (1200, 1600), INK)
    draw = ImageDraw.Draw(preview)
    label_font = ImageFont.truetype(str(BODY), 28)
    for kind in SPECS:
        svg, boxes = compose(kind)
        size, safe = SPECS[kind]["size"], SPECS[kind]["safe"]
        for label, (left, top, right, bottom) in boxes:
            if not (safe[0] <= left <= right <= safe[2] and safe[1] <= top <= bottom <= safe[3]):
                raise ValueError(f"{kind}: {label} outside art safe area: {(left, top, right, bottom)}")
        master = HERE / f"{kind}.svg"
        master.write_text(svg)
        destination = output / f"val-esports-{kind}-{size[0]}x{size[1]}.png"
        subprocess.run(["rsvg-convert", str(master), "-o", str(destination)], check=True)
        with Image.open(destination) as image:
            image.convert("RGB").save(destination, optimize=True)
        with Image.open(destination) as image:
            if image.size != size or image.mode != "RGB":
                raise ValueError(f"invalid {destination}")
            thumb = image.copy()
            thumb.thumbnail((1120, 750), Image.Resampling.LANCZOS)
            y = 65 if kind == "header" else 790
            draw.text((40, y-40), f"{kind.capitalize()} / {size[0]} x {size[1]}", fill=IVORY, font=label_font)
            preview.paste(thumb, (40, y))
            safe_crop = image.crop(safe)
            safe_crop.thumbnail((1120, 330), Image.Resampling.LANCZOS)
            review = Image.new("RGB", (1200, safe_crop.height+120), INK)
            ImageDraw.Draw(review).text((40, 25), "Apple art safe area / crop check", fill=IVORY, font=label_font)
            review.paste(safe_crop, (40, 80))
            review.save(output / f"review-{kind}-safe-area.png")
        records.append({
            "placement": kind, "filename": destination.name, "dimensions": size,
            "mode": "RGB", "alpha": False, "art_safe_area": safe,
            "essential_content_within_safe_area": True,
            "sha256": hashlib.sha256(destination.read_bytes()).hexdigest(),
        })
    preview.save(output / "preview.png")
    report = {
        "assets": records,
        "source_capture": str(CAPTURE.relative_to(ROOT)),
        "fonts": {
            "headings": str(HEADLINE.relative_to(ROOT)),
            "supporting_text": str(BODY.relative_to(ROOT)),
        },
        "capture_sha256": hashlib.sha256(CAPTURE.read_bytes()).hexdigest(),
        "specifications": "https://developer.apple.com/help/app-store-connect/reference/app-information/creative-assets-specifications",
        "template_safe_areas": {
            "header": "https://devimages-cdn.apple.com/design/resources/download/app-store/creative_assets-product_page_header_template-static.psd",
            "search": "https://devimages-cdn.apple.com/design/resources/download/app-store/creative_assets-search_results_template-static.psd",
        },
    }
    (output / "validation.json").write_text(json.dumps(report, indent=2) + "\n")
    with zipfile.ZipFile(HERE / "upload-assets.zip", "w", zipfile.ZIP_DEFLATED) as archive:
        for record in records:
            archive.write(output / record["filename"], record["filename"])
    print(f"Rendered and validated {len(records)} opaque PNG assets in {output}")


if __name__ == "__main__":
    main()
