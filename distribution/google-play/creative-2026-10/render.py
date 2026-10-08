"""Render Play graphics in the App Store lilac pixel design using genuine Android UI."""

import base64
import hashlib
import importlib.util
import json
import subprocess
from pathlib import Path

from fontTools.ttLib import TTFont
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
SOURCE = ROOT / 'distribution/rebrand-2026/raw/android'
DESIGN = ROOT / 'distribution/appstore/creative-2026-10/render.py'
_spec = importlib.util.spec_from_file_location('appstore_creative', DESIGN)
brand = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(brand)


def panel(x, y, width, height, fill, step=16):
    return (f'<path fill="{fill}" d="M{x+step} {y}H{x+width-step}V{y+step}'
            f'H{x+width}V{y+height-step}H{x+width-step}V{y+height}'
            f'H{x+step}V{y+height-step}H{x}V{y+step}H{x+step}Z"/>')


def background(width, height):
    return (f'<rect width="{width}" height="{height}" fill="{brand.LILAC}"/>'
            f'<path fill="{brand.PINK}" d="M0 0H160V80H240V160H320V240H240V160H160V80H0Z"/>'
            f'<path fill="{brand.BLUE}" d="M{width} {height}H{width-160}V{height-80}'
            f'H{width-240}V{height-160}H{width-320}V{height-240}H{width-240}'
            f'V{height-160}H{width-160}V{height-80}H{width}Z"/>')


def text(label, font, size, x, baseline, bounds):
    return brand.text_path(label, font, size, x, baseline, brand.INK, bounds)


def capture(number, x, y, width):
    path = SOURCE / f'{number:02}.png'
    with Image.open(path) as image:
        sw, sh = image.size
    height = width * sh / sw
    payload = base64.b64encode(path.read_bytes()).decode()
    return (f'<image x="{x}" y="{y}" width="{width}" height="{height}" '
            f'href="data:image/png;base64,{payload}"/>', (x, y, x+width, y+height), path)


def feature(locale, config):
    size, bounds = (1024, 500), []
    parts = [background(*size), panel(64, 64, 912, 388, '#AD8BD8'),
             panel(48, 48, 912, 388, brand.IVORY), brand.icon(124, 179, 132)]
    parts.append(text('Val Esports', brand.HEADLINE, 68, 292, 249, bounds))
    parts.append(text(config['feature_support'], brand.BODY, 24, 295, 302, bounds))
    parts.append(text(config['feature_footer'], brand.BODY, 19, 295, 348, bounds))
    bounds.append(('shipping icon', (124, 179, 256, 311)))
    return parts, size, bounds, None


def phone(locale, slide):
    size, bounds = (1080, 1920), []
    parts = [background(*size), brand.icon(70, 66, 46)]
    parts.append(text('Val Esports', brand.BODY, 27, 132, 101, bounds))
    for line, baseline in zip(slide['headline'], (203, 284)):
        parts.append(text(line, brand.HEADLINE, 76, 70, baseline, bounds))
    parts.append(text(slide['support'], brand.BODY, 32, 72, 344, bounds))
    parts += [panel(204, 392, 696, 1486, '#AD8BD8'), panel(188, 376, 696, 1486, brand.INK)]
    ui, box, path = capture(slide['source'], 206, 394, 660)
    parts.append(ui)
    bounds.append(('genuine Android screen', box))
    return parts, size, bounds, path


def render_svg(parts, size, name, output):
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size[0]}" height="{size[1]}" '
           f'viewBox="0 0 {size[0]} {size[1]}">' + ''.join(parts) + '</svg>')
    master = output / 'masters' / (name + '.svg')
    master.parent.mkdir(parents=True, exist_ok=True)
    master.write_text(svg)
    result = output / (name + '.png')
    subprocess.run(['rsvg-convert', str(master), '-o', str(result)], check=True)
    with Image.open(result) as image:
        image.convert('RGB').save(result, optimize=True)
    with Image.open(result) as image:
        image.load()
        assert image.size == size and image.mode == 'RGB'
    return result


def main():
    config = json.loads((HERE / 'manifest.json').read_text())
    output = HERE / 'output'
    output.mkdir(exist_ok=True)
    font_cmaps = {str(font): set(TTFont(font).getBestCmap()) for font in (brand.HEADLINE, brand.BODY)}
    records = []
    for locale, localized in config['locales'].items():
        folder = output / locale
        folder.mkdir(exist_ok=True)
        graphics = [('feature-graphic-1024x500', feature(locale, localized))]
        for index, slide in enumerate(localized['slides'], 1):
            assert 1 <= len(slide['headline']) <= 2
            graphics.append((f'{index:02}', phone(locale, slide)))
        for name, (parts, size, boxes, source) in graphics:
            for label, (left, top, right, bottom) in boxes:
                if not (40 <= left <= right <= size[0]-40 and 40 <= top <= bottom <= size[1]-40):
                    raise ValueError(f'{locale}/{name}: text or UI outside safe margins: {label}')
                supported = font_cmaps[str(brand.HEADLINE)] | font_cmaps[str(brand.BODY)]
                if name != 'shipping icon' and any(ord(ch) not in supported for ch in label):
                    raise ValueError(f'Unsupported glyphs: {label}')
            file = render_svg(parts, size, name, folder)
            records.append({'locale': locale, 'file': str(file.relative_to(HERE)), 'dimensions': size,
                            'mode': 'RGB', 'alpha': False, 'source_capture': str(source.relative_to(ROOT)) if source else None,
                            'source_capture_sha256': hashlib.sha256(source.read_bytes()).hexdigest() if source else None,
                            'sha256': hashlib.sha256(file.read_bytes()).hexdigest(), 'bounds_checked': True})
        contact = Image.new('RGB', (1420, 950), brand.INK)
        with Image.open(folder / 'feature-graphic-1024x500.png') as image:
            image.thumbnail((820, 400), Image.Resampling.LANCZOS)
            contact.paste(image, (30, 30))
        ImageDraw.Draw(contact).text((900, 85), locale, fill=brand.IVORY, font=ImageFont.truetype(str(brand.BODY), 40))
        ImageDraw.Draw(contact).text((900, 145), 'Android UI', fill=brand.IVORY, font=ImageFont.truetype(str(brand.BODY), 27))
        for index in range(1, 6):
            with Image.open(folder / f'{index:02}.png') as image:
                image.thumbnail((260, 463), Image.Resampling.LANCZOS)
                contact.paste(image, (30 + (index-1)*276, 460))
        contact.save(folder / 'contact-sheet.png')
    report = {'assets': records, 'fonts': {'headings': str(brand.HEADLINE.relative_to(ROOT)),
                                         'body': str(brand.BODY.relative_to(ROOT))},
              'font_fallback_used': False, 'source_ui_language': 'English',
              'hindi_art_language': 'English', 'hindi_art_reason': 'Bundled brand fonts have no Devanagari glyph coverage.',
              'feature_graphics': 4, 'phone_screenshots': 20}
    (output / 'validation.json').write_text(json.dumps(report, indent=2) + '\n')
    print(f'Rendered and validated {len(records)} opaque assets.')


if __name__ == '__main__':
    main()
