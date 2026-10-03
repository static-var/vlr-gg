#!/usr/bin/env python3
"""Render deterministic Valorant Esports store artwork from real app captures."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFilter, ImageFont


RESAMPLE = Image.Resampling.LANCZOS


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=Path(__file__).with_name("manifest.json"))
    parser.add_argument("--output", type=Path, default=Path(__file__).parent.parent / "output" / "final")
    parser.add_argument(
        "--preview",
        action="store_true",
        help="Use preview_sources when fresh captures are absent and label every affected output.",
    )
    parser.add_argument(
        "--include-ipad",
        action="store_true",
        help="Also render the optional 2064x2752 iPad set from real ios_ipad captures.",
    )
    parser.add_argument(
        "--include-play-tablet",
        action="store_true",
        help="Also render the optional 1920x1080 Play tablet set from real android_tablet captures.",
    )
    parser.add_argument(
        "--target",
        action="append",
        choices=("play", "app_store", "play_tablet", "app_store_ipad", "wide"),
        help="Render only the selected target. Repeat for multiple targets. By default, render both phone stores and wide assets.",
    )
    return parser.parse_args()


def load_manifest(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as handle:
        data = json.load(handle)
    if len(data.get("slides", [])) != 5:
        raise ValueError("manifest must define exactly five slides")
    return data


def color(value: str, alpha: int | None = None) -> tuple[int, ...]:
    value = value.lstrip("#")
    if len(value) != 6:
        raise ValueError(f"expected a six-digit hex color, got {value!r}")
    rgb = tuple(int(value[index : index + 2], 16) for index in (0, 2, 4))
    return (*rgb, alpha) if alpha is not None else rgb


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, max(1, size))


def text_width(draw: ImageDraw.ImageDraw, value: str, typeface: ImageFont.FreeTypeFont) -> float:
    left, _, right, _ = draw.textbbox((0, 0), value, font=typeface)
    return right - left


def wrap_text(draw: ImageDraw.ImageDraw, value: str, typeface: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    words = value.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = f"{current} {word}".strip()
        if current and text_width(draw, candidate, typeface) > max_width:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines


def fit_headline(
    draw: ImageDraw.ImageDraw,
    value: str,
    typeface_path: str,
    max_width: int,
    max_lines: int,
    initial_size: int,
) -> tuple[ImageFont.FreeTypeFont, list[str]]:
    for size in range(initial_size, max(24, initial_size // 2), -2):
        typeface = font(typeface_path, size)
        lines = wrap_text(draw, value, typeface, max_width)
        if len(lines) <= max_lines:
            return typeface, lines
    raise ValueError(f"headline cannot fit in {max_lines} lines: {value!r}")


def draw_tracking_text(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int],
    value: str,
    typeface: ImageFont.FreeTypeFont,
    fill: tuple[int, ...],
    tracking: float,
) -> None:
    x, y = xy
    for character in value:
        draw.text((round(x), y), character, font=typeface, fill=fill)
        x += text_width(draw, character, typeface) + tracking


def draw_registration_mark(draw: ImageDraw.ImageDraw, cx: int, cy: int, radius: int, stroke: tuple[int, ...], width: int) -> None:
    draw.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), outline=stroke, width=width)
    draw.line((cx - radius * 2, cy, cx + radius * 2, cy), fill=stroke, width=width)
    draw.line((cx, cy - radius * 2, cx, cy + radius * 2), fill=stroke, width=width)


def draw_motif(draw: ImageDraw.ImageDraw, motif: str, width: int, height: int, stroke: tuple[int, ...]) -> None:
    line = max(2, width // 500)
    if motif == "court":
        left, top, right, bottom = int(width * 0.58), int(height * 0.05), int(width * 1.04), int(height * 0.42)
        draw.rectangle((left, top, right, bottom), outline=stroke, width=line)
        draw.line((left, (top + bottom) // 2, right, (top + bottom) // 2), fill=stroke, width=line)
        draw.arc((left + int(width * 0.1), top + int(height * 0.05), right - int(width * 0.1), bottom - int(height * 0.05)), 0, 360, fill=stroke, width=line)
    elif motif == "target":
        draw_registration_mark(draw, int(width * 0.87), int(height * 0.15), int(width * 0.12), stroke, line)
        draw_registration_mark(draw, int(width * 0.12), int(height * 0.82), int(width * 0.08), stroke, line)
    elif motif == "ticket":
        step = max(24, width // 15)
        for x in range(-step, width + step, step):
            draw.line((x, 0, x + int(width * 0.62), height), fill=stroke, width=line)
        draw.rectangle((int(width * 0.61), int(height * 0.06), int(width * 0.94), int(height * 0.23)), outline=stroke, width=line)
    elif motif == "bracket":
        x0, x1, x2 = int(width * 0.65), int(width * 0.79), int(width * 0.93)
        ys = [int(height * value) for value in (0.06, 0.13, 0.2, 0.27)]
        for index, y in enumerate(ys):
            draw.line((x0, y, x1, y), fill=stroke, width=line)
            partner = ys[index ^ 1]
            mid = (y + partner) // 2
            draw.line((x1, min(y, partner), x1, max(y, partner)), fill=stroke, width=line)
            draw.line((x1, mid, x2, mid), fill=stroke, width=line)
    elif motif == "rank":
        base = int(height * 0.27)
        for index, ratio in enumerate((0.08, 0.14, 0.22, 0.32)):
            x = int(width * (0.60 + index * 0.09))
            draw.rectangle((x, base - int(height * ratio), x + int(width * 0.055), base), outline=stroke, width=line)


def theme_colors(theme: str, palette: dict[str, str]) -> dict[str, tuple[int, ...]]:
    if theme == "ivory":
        return {
            "background": color(palette["ivory"]),
            "text": color(palette["ink"]),
            "accent": color(palette["violet"]),
            "motif": color(palette["violet"], 78),
            "frame": color(palette["ink"]),
        }
    if theme == "ink":
        return {
            "background": color(palette["ink"]),
            "text": color(palette["ivory"]),
            "accent": color(palette["violet"]),
            "motif": color(palette["violet"], 95),
            "frame": color(palette["ivory"]),
        }
    if theme == "violet":
        return {
            "background": color(palette["violet"]),
            "text": color(palette["ink"]),
            "accent": color(palette["ivory"]),
            "motif": color(palette["ink"], 58),
            "frame": color(palette["ink"]),
        }
    raise ValueError(f"unknown slide theme: {theme}")


def resolve_source(manifest_path: Path, slide: dict[str, Any], platform: str, preview: bool) -> tuple[Path, bool]:
    root = manifest_path.parent
    source = (root / slide["sources"][platform]).resolve()
    if source.is_file():
        return source, False
    if preview:
        fallback_value = slide.get("preview_sources", {}).get(platform)
        if fallback_value:
            fallback = (root / fallback_value).resolve()
            if fallback.is_file():
                return fallback, True
    raise FileNotFoundError(f"missing {platform} capture for slide {slide['id']}: {source}")


def fit_inside(source_size: tuple[int, int], bounds: tuple[int, int]) -> tuple[int, int]:
    source_width, source_height = source_size
    max_width, max_height = bounds
    scale = min(max_width / source_width, max_height / source_height)
    return max(1, round(source_width * scale)), max(1, round(source_height * scale))


def add_device_frame(
    canvas: Image.Image,
    capture: Image.Image,
    box: tuple[int, int, int, int],
    frame_color: tuple[int, ...],
    accent: tuple[int, ...],
    device: dict[str, Any] | None = None,
) -> tuple[int, int, int, int, int, int]:
    if device is not None:
        return add_product_bezel(canvas, capture, box, device)
    x, y, max_width, max_height = box
    inner_width, inner_height = fit_inside(capture.size, (max_width, max_height))
    border = max(10, round(inner_width * 0.022))
    outer_width, outer_height = inner_width + border * 2, inner_height + border * 2
    left = x + (max_width - outer_width) // 2
    top = y + (max_height - outer_height) // 2
    radius = max(18, round(inner_width * 0.045))

    shadow_layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    shadow = ImageDraw.Draw(shadow_layer)
    offset = max(12, border)
    shadow.rounded_rectangle(
        (left + offset, top + offset, left + outer_width + offset, top + outer_height + offset),
        radius=radius,
        fill=(0, 0, 0, 92),
    )
    shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(max(10, border)))
    canvas.alpha_composite(shadow_layer)

    frame_layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    frame_draw = ImageDraw.Draw(frame_layer)
    frame_draw.rounded_rectangle(
        (left, top, left + outer_width, top + outer_height),
        radius=radius,
        fill=frame_color,
        outline=accent,
        width=max(2, border // 5),
    )
    canvas.alpha_composite(frame_layer)
    resized = capture.convert("RGB").resize((inner_width, inner_height), RESAMPLE).convert("RGBA")
    canvas.alpha_composite(resized, (left + border, top + border))
    return left, top, outer_width, outer_height, inner_width, inner_height


def add_product_bezel(
    canvas: Image.Image,
    capture: Image.Image,
    box: tuple[int, int, int, int],
    device: dict[str, Any],
) -> tuple[int, int, int, int, int, int]:
    bezel = Image.open(device["path"]).convert("RGBA")
    screen_x, screen_y, screen_width, screen_height = device["screen"]
    if abs(capture.width / capture.height - screen_width / screen_height) > 0.002:
        raise ValueError("capture aspect ratio does not match the selected device display")
    x, y, max_width, max_height = box
    outer_width, outer_height = fit_inside(bezel.size, (max_width, max_height))
    scale = outer_width / bezel.width
    left = x + (max_width - outer_width) // 2
    top = y + (max_height - outer_height) // 2
    capture_width, capture_height = fit_inside(capture.size, (round(screen_width * scale), round(screen_height * scale)))
    screen_left = left + round(screen_x * scale)
    screen_top = top + round(screen_y * scale)
    screen = capture.convert("RGBA").resize((capture_width, capture_height), RESAMPLE)
    mask = Image.open(device["screen_mask"]).convert("L").resize(screen.size, RESAMPLE)
    screen.putalpha(mask)
    canvas.alpha_composite(screen, (screen_left, screen_top))
    canvas.alpha_composite(bezel.resize((outer_width, outer_height), RESAMPLE), (left, top))
    return left, top, outer_width, outer_height, capture_width, capture_height


def render_slide(
    slide: dict[str, Any],
    spec: dict[str, Any],
    capture: Image.Image,
    used_preview: bool,
    brand: dict[str, Any],
) -> tuple[Image.Image, tuple[int, int]]:
    width, height = spec["width"], spec["height"]
    is_landscape = width > height
    palette = brand["palette"]
    colors = theme_colors(slide["theme"], palette)
    canvas = Image.new("RGBA", (width, height), colors["background"])
    motif_layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw_motif(ImageDraw.Draw(motif_layer), slide["motif"], width, height, colors["motif"])
    canvas.alpha_composite(motif_layer)
    draw = ImageDraw.Draw(canvas)

    margin = round(width * (0.055 if is_landscape else 0.075))
    kicker_font = font(brand["fonts"]["body_bold"], round(width * 0.023))
    meta_font = font(brand["fonts"]["body"], round(width * 0.018))
    meta_y = round(height * (0.065 if is_landscape else 0.048))
    draw_tracking_text(draw, (margin, meta_y), slide["kicker"], kicker_font, colors["accent"], width * 0.0024)
    slide_number = f"{slide['id']} / 05"
    slide_number_width = text_width(draw, slide_number, meta_font)
    draw.text((width - margin - slide_number_width, meta_y), slide_number, font=meta_font, fill=colors["text"])

    title_size = round(width * (0.070 if is_landscape else (0.105 if height / width < 2.0 else 0.112)))
    title_max_width = round(width * 0.39) if is_landscape else width - margin * 2
    title_font, title_lines = fit_headline(
        draw,
        slide["headline"],
        brand["fonts"]["headline"],
        title_max_width,
        3 if is_landscape else 2,
        title_size,
    )
    title_y = round(height * (0.18 if is_landscape else 0.095))
    title_spacing = round(title_font.size * 0.89)
    for line in title_lines:
        draw.text((margin, title_y), line, font=title_font, fill=colors["text"], stroke_width=0)
        title_y += title_spacing

    rule_y = round(height * (0.78 if is_landscape else (0.29 if height / width < 2.0 else 0.245)))
    rule_end = round(width * 0.40) if is_landscape else width - margin
    draw.rectangle((margin, rule_y, margin + round(width * 0.12 if is_landscape else width * 0.16), rule_y + max(5, width // 150)), fill=colors["accent"])
    draw.line((margin + round(width * 0.145 if is_landscape else width * 0.185), rule_y + 2, rule_end, rule_y + 2), fill=colors["text"], width=max(1, width // 700))

    if is_landscape:
        device_box = (round(width * 0.47), round(height * 0.12), round(width * 0.46), round(height * 0.76))
    else:
        device_top = round(height * (0.31 if height / width < 2.0 else 0.265))
        device_bottom_margin = round(height * (0.012 if height / width < 2.0 else 0.005))
        device_box = (margin, device_top, width - margin * 2, height - device_top - device_bottom_margin)
    frame = add_device_frame(
        canvas,
        capture,
        device_box,
        colors["frame"],
        colors["accent"],
        spec.get("device_frame"),
    )

    frame_left, frame_top, frame_width, frame_height, capture_width, capture_height = frame
    corner = round(width * 0.038)
    bracket_width = max(4, width // 250)
    for x1, y1, sx, sy in (
        (frame_left - corner, frame_top - corner, 1, 1),
        (frame_left + frame_width + corner, frame_top - corner, -1, 1),
        (frame_left - corner, frame_top + frame_height + corner, 1, -1),
        (frame_left + frame_width + corner, frame_top + frame_height + corner, -1, -1),
    ):
        draw.line((x1, y1, x1 + sx * corner, y1), fill=colors["accent"], width=bracket_width)
        draw.line((x1, y1, x1, y1 + sy * corner), fill=colors["accent"], width=bracket_width)

    if used_preview:
        preview_height = round(height * 0.033)
        overlay = Image.new("RGBA", (width, preview_height), color(palette["violet"], 235))
        canvas.alpha_composite(overlay, (0, 0))
        preview_font = font(brand["fonts"]["body_bold"], round(width * 0.019))
        label = "TEMPLATE PREVIEW / REPLACE CAPTURE BEFORE PUBLISHING"
        label_width = text_width(draw, label, preview_font)
        draw.text(((width - label_width) / 2, preview_height * 0.22), label, font=preview_font, fill=color(palette["ink"]))

    return canvas.convert("RGB"), (capture_width, capture_height)


def render_wide_asset(asset: dict[str, Any], brand: dict[str, Any], variant: str) -> Image.Image:
    width, height = asset["width"], asset["height"]
    palette = brand["palette"]
    background = color(palette["ink"] if variant == "feature_graphic" else palette["ivory"])
    foreground = color(palette["ivory"] if variant == "feature_graphic" else palette["ink"])
    accent = color(palette["violet"])
    canvas = Image.new("RGBA", (width, height), background)
    draw = ImageDraw.Draw(canvas)

    grid_layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    grid = ImageDraw.Draw(grid_layer)
    grid_color = (*accent, 64)
    step = max(36, width // 16)
    for x in range(0, width + step, step):
        grid.line((x, 0, x + height, height), fill=grid_color, width=max(1, width // 800))
    canvas.alpha_composite(grid_layer)

    margin = round(width * 0.06)
    badge_font = font(brand["fonts"]["body_bold"], round(width * 0.018))
    draw_tracking_text(draw, (margin, round(height * 0.105)), brand["descriptor"], badge_font, accent, width * 0.0018)
    headline_font, lines = fit_headline(
        draw,
        asset["headline"],
        brand["fonts"]["headline"],
        round(width * 0.68),
        2,
        round(width * 0.072),
    )
    y = round(height * 0.22)
    for line_text in lines:
        draw.text((margin, y), line_text, font=headline_font, fill=foreground)
        y += round(headline_font.size * 0.9)
    kicker_font = font(brand["fonts"]["body_bold"], round(width * 0.019))
    draw_tracking_text(draw, (margin, round(height * 0.77)), asset["kicker"], kicker_font, foreground, width * 0.0017)
    draw.rectangle((margin, round(height * 0.86), margin + round(width * 0.13), round(height * 0.875)), fill=accent)

    symbol_left = round(width * 0.79)
    symbol_top = round(height * 0.16)
    symbol_size = round(min(width, height) * 0.52)
    draw.rectangle((symbol_left, symbol_top, symbol_left + symbol_size, symbol_top + symbol_size), outline=accent, width=max(5, width // 170))
    draw.line((symbol_left, symbol_top + symbol_size // 2, symbol_left + symbol_size, symbol_top + symbol_size // 2), fill=accent, width=max(3, width // 260))
    draw.line((symbol_left + symbol_size // 2, symbol_top, symbol_left + symbol_size // 2, symbol_top + symbol_size), fill=accent, width=max(3, width // 260))
    draw.ellipse((symbol_left + symbol_size * 0.32, symbol_top + symbol_size * 0.32, symbol_left + symbol_size * 0.68, symbol_top + symbol_size * 0.68), outline=foreground, width=max(4, width // 210))
    return canvas.convert("RGB")


def make_contact_sheet(paths: list[Path], output: Path, label: str, brand: dict[str, Any]) -> None:
    thumbs: list[Image.Image] = []
    thumb_width = 300
    for path in paths:
        image = Image.open(path).convert("RGB")
        ratio = thumb_width / image.width
        thumbs.append(image.resize((thumb_width, round(image.height * ratio)), RESAMPLE))
    gap, margin, header = 24, 36, 96
    width = margin * 2 + len(thumbs) * thumb_width + (len(thumbs) - 1) * gap
    height = header + margin + max(item.height for item in thumbs) + margin
    palette = brand["palette"]
    sheet = Image.new("RGB", (width, height), color(palette["ink"]))
    draw = ImageDraw.Draw(sheet)
    title_font = font(brand["fonts"]["body_bold"], 28)
    draw_tracking_text(draw, (margin, 31), label.upper(), title_font, color(palette["ivory"]), 2.0)
    x = margin
    for item in thumbs:
        sheet.paste(item, (x, header))
        x += thumb_width + gap
    output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(output, optimize=True)


def main() -> int:
    args = parse_args()
    manifest_path = args.manifest.resolve()
    data = load_manifest(manifest_path)
    output_root = args.output.resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    records: list[dict[str, Any]] = []
    all_portrait_paths: list[Path] = []

    requested_targets = set(args.target or [])
    output_specs = dict(data["outputs"] if not requested_targets else {})
    optional_outputs = data.get("optional_outputs", {})
    for target in requested_targets - {"wide"}:
        if target in data["outputs"]:
            output_specs[target] = data["outputs"][target]
        else:
            output_specs[target] = optional_outputs[target]
    if args.include_play_tablet:
        output_specs["play_tablet"] = optional_outputs["play_tablet"]
    if args.include_ipad:
        output_specs["app_store_ipad"] = optional_outputs["app_store_ipad"]
    for output_name, spec in output_specs.items():
        if "device_frame" in spec:
            spec["device_frame"]["path"] = str((manifest_path.parent / spec["device_frame"]["path"]).resolve())
            spec["device_frame"]["screen_mask"] = str((manifest_path.parent / spec["device_frame"]["screen_mask"]).resolve())
        output_dir = output_root / spec["directory"]
        output_dir.mkdir(parents=True, exist_ok=True)
        platform_paths: list[Path] = []
        for slide in data["slides"]:
            source_path, used_preview = resolve_source(manifest_path, slide, spec["platform"], args.preview)
            with Image.open(source_path) as capture:
                original_size = capture.size
                rendered, rendered_capture_size = render_slide(slide, spec, capture.copy(), used_preview, data["brand"])
            destination = output_dir / f"{slide['id']}.png"
            rendered.save(destination, optimize=True)
            platform_paths.append(destination)
            all_portrait_paths.append(destination)
            records.append(
                {
                    "asset": f"{output_name}/{slide['id']}",
                    "path": str(destination),
                    "dimensions": list(rendered.size),
                    "source": str(source_path),
                    "source_dimensions": list(original_size),
                    "rendered_capture_dimensions": list(rendered_capture_size),
                    "preview_source": used_preview,
                    "aspect_ratio_preserved": True,
                    **({"device_frame": spec["device_frame"]} if "device_frame" in spec else {}),
                }
            )
        make_contact_sheet(platform_paths, output_root / f"contact-sheet-{output_name}.png", output_name.replace("_", " "), data["brand"])

    if not requested_targets or "wide" in requested_targets:
        for asset_name, asset in data["wide_assets"].items():
            rendered = render_wide_asset(asset, data["brand"], asset_name)
            destination = output_root / asset["filename"]
            rendered.save(destination, optimize=True)
            records.append(
                {
                    "asset": asset_name,
                    "path": str(destination),
                    "dimensions": list(rendered.size),
                    "preview_source": False,
                }
            )

    if all_portrait_paths:
        make_contact_sheet(all_portrait_paths, output_root / "contact-sheet-all.png", "Store screenshot sequence", data["brand"])
    report = {
        "manifest": str(manifest_path),
        "mode": "preview" if args.preview else "final",
        "asset_count": len(records),
        "assets": records,
    }
    with (output_root / "validation.json").open("w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)
        handle.write("\n")
    print(f"Rendered {len(records)} assets to {output_root}")
    print(f"Validation: {output_root / 'validation.json'}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (FileNotFoundError, ValueError) as error:
        print(f"render error: {error}", file=sys.stderr)
        raise SystemExit(2)
