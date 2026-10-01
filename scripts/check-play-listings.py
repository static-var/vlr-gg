#!/usr/bin/env python3
"""Validate Play listing text and optionally export a translation import file."""

import argparse
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
PLAY_LOCALES = {
    "en": "en-US", "pt-BR": "pt-BR", "hi": "hi-IN", "tr": "tr-TR",
    "es": "es-ES", "fr": "fr-FR", "de": "de-DE", "ko": "ko-KR", "ru": "ru-RU",
}
LIMITS = {"title": 30, "shortDescription": 80, "fullDescription": 4000}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--export", type=Path, help="Write non-English translations for Play Console import")
    args = parser.parse_args()
    config = ET.parse(ROOT / "androidApp/src/main/res/xml/locales_config.xml")
    app_locales = {node.attrib["{http://schemas.android.com/apk/res/android}name"] for node in config.getroot()}
    unknown = app_locales - PLAY_LOCALES.keys()
    if unknown:
        parser.error(f"Missing Play locale mapping: {sorted(unknown)}")
    expected = {PLAY_LOCALES[locale] for locale in app_locales}
    directory = ROOT / "distribution/play-listings"
    actual = {path.stem for path in directory.glob("*.json")}
    errors = []
    if actual != expected:
        errors.append(f"Locale mismatch: missing={sorted(expected - actual)}, extra={sorted(actual - expected)}")
    listings = {}
    for locale in sorted(actual):
        try:
            listing = json.loads((directory / f"{locale}.json").read_text(encoding="utf-8"))
        except (OSError, ValueError) as error:
            errors.append(f"{locale}: {error}")
            continue
        if not isinstance(listing, dict) or listing.keys() != LIMITS.keys():
            errors.append(f"{locale}: expected fields {list(LIMITS)}")
            continue
        for field, limit in LIMITS.items():
            value = listing[field]
            if not isinstance(value, str) or not value.strip():
                errors.append(f"{locale}/{field}: must be nonempty text")
            elif len(value) > limit:
                errors.append(f"{locale}/{field}: {len(value)} characters exceeds {limit}")
        if listing["title"] != "Val Esports":
            errors.append(f"{locale}: app name must be Val Esports")
        if isinstance(listing["fullDescription"], str):
            for name in ("Riot Games", "VLR.gg", "Valorant"):
                if name not in listing["fullDescription"]:
                    errors.append(f"{locale}: missing {name} attribution")
        listings[locale] = listing
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    for locale, listing in listings.items():
        counts = ", ".join(f"{field}={len(listing[field])}/{limit}" for field, limit in LIMITS.items())
        print(f"{locale}: {counts}")
    if args.export:
        translations = {locale: listing for locale, listing in listings.items() if locale != "en-US"}
        args.export.write_text(json.dumps(translations, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"Exported {len(translations)} translations to {args.export}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
