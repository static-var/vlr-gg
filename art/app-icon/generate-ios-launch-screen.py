#!/usr/bin/env python3
"""Build the iOS launch screen artwork from the Match point master and the app fonts.

Requires Python 3 and fontTools. Launch storyboards cannot load bundled fonts, so text is outlined.
Run from any directory: python3 art/app-icon/generate-ios-launch-screen.py
"""

import json
from pathlib import Path
import re

from outlined_text import outlined_text


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "art/app-icon/match-point.svg"
ASSETS = ROOT / "iosApp/iosApp/Assets.xcassets"
# Launch background, text, caption, and accent colors mirror the design system's color tokens.
THEMES = {
    "light": {"background": "FFFFFF", "text": "#000000", "caption": "#626262", "accent": "#7C3AED"},
    "dark": {"background": "0A0A0A", "text": "#FFFFFF", "caption": "#A3A3A3", "accent": "#A78BFA"},
}
LOGO_SIZE = (280, 240)
CAPTION_SIZE = (260, 16)
# 3.5 pt per master unit keeps the master's two-unit pixel grid on whole points.
ICON_TRANSFORM = "translate(28 -38) scale(3.5)"


def svg(size, body):
    width, height = size
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}">\n{body}\n</svg>\n'
    )


def logo(theme, foreground):
    wordmark = outlined_text("VAL ESPORTS", "chakra_petch_regular.ttf", 30, 7, 140, 212)
    return svg(
        LOGO_SIZE,
        f'<g transform="{ICON_TRANSFORM}">{foreground}</g>\n<path fill="{theme["text"]}" d="{wordmark}"/>\n'
        f'<path fill="{theme["accent"]}" d="M124 232h38l-5 5h-38z"/>',
    )


def caption(theme):
    text = outlined_text("// MATCHES · EVENTS · RANKINGS", "space_grotesk_regular.ttf", 11, 2.2, 130, 12)
    return svg(CAPTION_SIZE, f'<path fill="{theme["caption"]}" d="{text}"/>')


def dark_appearance():
    return [{"appearance": "luminosity", "value": "dark"}]


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n")


def write_imageset(name, stem, render):
    directory = ASSETS / f"{name}.imageset"
    directory.mkdir(parents=True, exist_ok=True)
    images = []
    for mode, theme in THEMES.items():
        filename = f"{stem}-{mode}.svg"
        (directory / filename).write_text(render(theme))
        image = {"filename": filename, "idiom": "universal"}
        if mode == "dark":
            image["appearances"] = dark_appearance()
        images.append(image)
    write_json(
        directory / "Contents.json",
        {
            "images": images,
            "info": {"author": "xcode", "version": 1},
            "properties": {"preserves-vector-representation": True},
        },
    )


def write_background():
    colors = []
    for mode, theme in THEMES.items():
        red, green, blue = (f"0x{theme['background'][index:index + 2]}" for index in (0, 2, 4))
        color = {
            "color": {
                "color-space": "srgb",
                "components": {"alpha": "1.000", "blue": blue, "green": green, "red": red},
            },
            "idiom": "universal",
        }
        if mode == "dark":
            color["appearances"] = dark_appearance()
        colors.append(color)
    directory = ASSETS / "LaunchBackground.colorset"
    directory.mkdir(parents=True, exist_ok=True)
    write_json(directory / "Contents.json", {"colors": colors, "info": {"author": "xcode", "version": 1}})


def main():
    foreground = re.search(r'<g id="foreground"[^>]*>(.*?)</g>', SOURCE.read_text(), re.S).group(1)
    foreground = "".join(line.strip() for line in foreground.splitlines())
    write_imageset("LaunchLogo", "launch-logo", lambda theme: logo(theme, foreground))
    write_imageset("LaunchCaption", "launch-caption", caption)
    write_background()


if __name__ == "__main__":
    main()
