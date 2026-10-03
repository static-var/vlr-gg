"""Outline text with the app fonts for launch artwork that cannot load bundled fonts."""

from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont


FONTS = Path(__file__).resolve().parents[2] / "designsystem/src/commonMain/composeResources/font"


def number(value):
    return f"{value:.2f}".rstrip("0").rstrip(".")


def outlined_text(text, font_name, size, spacing, center_x, baseline):
    """Return SVG path data for text centered on center_x with its baseline at baseline."""
    font = TTFont(FONTS / font_name)
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    scale = size / font["head"].unitsPerEm
    names = [cmap[ord(character)] for character in text]
    # The trailing letter gap is excluded so the visible glyphs are what gets centered.
    width = sum(glyphs[name].width * scale + spacing for name in names) - spacing
    x = center_x - width / 2
    commands = []
    for name in names:
        pen = SVGPathPen(glyphs, number)
        glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, x, baseline)))
        commands.append(pen.getCommands())
        x += glyphs[name].width * scale + spacing
    return "".join(commands)
