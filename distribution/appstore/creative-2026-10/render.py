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
INK, IVORY, VIOLET, LILAC = "#1E1E2E", "#F5F1E8", "#956DFF", "#CBA6F7"
PINK, BLUE = "#F38BA8", "#B4BEFE"
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
    if not (0 <= left < right <= sw and 0 <= top < bottom <= sh):
        raise ValueError(f"screenshot crop outside capture: {(left, top, right, bottom)}")
    height = width * (bottom - top) / (right - left)
    return (
        f'<svg x="{x}" y="{y}" width="{width}" height="{height}" '
        f'viewBox="{left} {top} {right-left} {bottom-top}" overflow="hidden">'
        f'<image width="{sw}" height="{sh}" href="data:image/png;base64,{payload}"/></svg>',
        (x, y, x + width, y + height),
    )


def pixel_panel(x, y, width, height, fill):
    step = 48
    return (
        f'<path fill="{fill}" d="M{x+step} {y}H{x+width-step}V{y+step}'
        f'H{x+width}V{y+height-step}H{x+width-step}V{y+height}'
        f'H{x+step}V{y+height-step}H{x}V{y+step}H{x+step}Z"/>'
    )


def background(width, height, kind):
    # The two pixel strokes echo the shipping icon's V without imitating gameplay.
    return f'''<rect width="{width}" height="{height}" fill="{LILAC}"/>
    <g fill="{PINK}">
      <path d="M0 0H320V240H480V480H640V720H800V960H960V1200H800V1040H640V800H480V560H320V320H160V160H0Z"/>
    </g>
    <g fill="{BLUE}">
      <path d="M{width} {height}H{width-320}V{height-240}H{width-480}V{height-480}H{width-640}V{height-720}H{width-800}V{height-960}H{width-960}V{height-1200}H{width-800}V{height-1040}H{width-640}V{height-800}H{width-480}V{height-560}H{width-320}V{height-320}H{width-160}V{height-160}H{width}Z"/>
    </g>'''


def compose(kind):
    spec = SPECS[kind]
    width, height = spec["size"]
    boxes = []
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
             background(width, height, kind)]
    if kind == "header":
        parts += [pixel_panel(808, 368, 2280, 976, "#AD8BD8"),
                  pixel_panel(760, 320, 2280, 976, IVORY)]
        parts.append(icon(1170, 679, 280))
        boxes.append(("shipping icon", (1170, 679, 1450, 959)))
        for label, font, size, x, baseline, fill in [
            ("Val Esports", HEADLINE, 225, 1490, 839, INK),
            ("VALORANT scores, schedules & stats", BODY, 56, 1493, 1000, INK),
        ]:
            parts.append(text_path(label, font, size, x, baseline, fill, boxes))
    else:
        parts += [pixel_panel(688, 608, 2560, 1440, "#AD8BD8"),
                  pixel_panel(640, 560, 2560, 1440, IVORY)]
        parts.append(icon(944, 1630, 88))
        boxes.append(("shipping icon", (944, 1630, 1032, 1718)))
        for label, font, size, x, baseline, fill in [
            ("Val", HEADLINE, 250, 940, 1030, INK),
            ("Esports", HEADLINE, 250, 930, 1270, INK),
            ("VALORANT scores", BODY, 60, 944, 1450, INK),
            ("Schedules & match stats", BODY, 54, 944, 1534, INK),
            ("Free. Ad-free. No sign-in.", BODY, 45, 1060, 1690, INK),
        ]:
            parts.append(text_path(label, font, size, x, baseline, fill, boxes))
        ui, bounds = screenshot(2120, 900, 820, (49, 474, 1157, 1408))
        parts += [pixel_panel(2144, 924, 820, 692, LILAC),
                  '<rect x="2116" y="896" width="828" height="700" fill="#1E1E2E"/>', ui]
        boxes.append(("actual match score UI", bounds))
    parts.append('</svg>')
    return "".join(parts), boxes


def main():
    output = HERE / "output"
    output.mkdir(exist_ok=True)
    records = []
    preview = Image.new("RGB", (1200, 1430), INK)
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
        destination = output / f"val-esports-{kind}-v2-{size[0]}x{size[1]}.png"
        subprocess.run(["rsvg-convert", str(master), "-o", str(destination)], check=True)
        with Image.open(destination) as image:
            image.convert("RGB").save(destination, optimize=True)
        with Image.open(destination) as image:
            if image.size != size or image.mode != "RGB":
                raise ValueError(f"invalid {destination}")
            thumb = image.copy()
            thumb.thumbnail((1120, 750), Image.Resampling.LANCZOS)
            y = 65 if kind == "header" else 635
            draw.text((40, y-40), f"{kind.capitalize()} / {size[0]} x {size[1]}", fill=IVORY, font=label_font)
            preview.paste(thumb, (40, y))
            safe_crop = image.crop(safe)
            safe_crop.thumbnail((1120, 560), Image.Resampling.LANCZOS)
            review = Image.new("RGB", (1200, safe_crop.height+120), INK)
            ImageDraw.Draw(review).text((40, 25), "Apple art safe area / crop check", fill=IVORY, font=label_font)
            review.paste(safe_crop, (40, 80))
            review.save(output / f"review-{kind}-safe-area.png")
        records.append({
            "placement": kind, "filename": destination.name, "dimensions": size,
            "mode": "RGB", "alpha": False, "art_safe_area": safe,
            "essential_content_within_safe_area": True,
            "content_bounds": {
                label: [round(min(bounds[i] for name, bounds in boxes if name == label), 2)
                        if i < 2 else round(max(bounds[i] for name, bounds in boxes if name == label), 2)
                        for i in range(4)]
                for label in dict.fromkeys(label for label, _ in boxes)
            },
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
