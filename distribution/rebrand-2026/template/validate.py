#!/usr/bin/env python3
"""Validate generated store artwork against its recorded contract."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    parser.add_argument("--allow-preview", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    report_path = args.output.resolve() / "validation.json"
    with report_path.open(encoding="utf-8") as handle:
        report = json.load(handle)

    errors: list[str] = []
    assets = report.get("assets", [])
    if report.get("asset_count") != len(assets):
        errors.append("asset_count does not match the asset records")
    if report.get("mode") == "preview" and not args.allow_preview:
        errors.append("preview output requires --allow-preview")

    for record in assets:
        path = Path(record["path"])
        if not path.is_file():
            errors.append(f"missing output: {path}")
            continue
        with Image.open(path) as image:
            actual_dimensions = list(image.size)
            if actual_dimensions != record["dimensions"]:
                errors.append(f"dimension mismatch for {path}: {actual_dimensions} != {record['dimensions']}")
            if image.mode != "RGB":
                errors.append(f"{path} uses {image.mode}; store PNGs must not have alpha")
            if image.format != "PNG":
                errors.append(f"{path} uses {image.format}; expected PNG")
        if record.get("preview_source") and not args.allow_preview:
            errors.append(f"{path} uses a preview source")
        if "source_dimensions" in record:
            source_width, source_height = record["source_dimensions"]
            render_width, render_height = record["rendered_capture_dimensions"]
            source_ratio = source_width / source_height
            render_ratio = render_width / render_height
            if abs(source_ratio - render_ratio) > 0.002:
                errors.append(f"capture aspect ratio changed for {path}: {source_ratio:.6f} -> {render_ratio:.6f}")

    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(f"Validated {len(assets)} store assets from {report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
