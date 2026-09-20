"""Verify the rendering contracts of the launcher SVG exporter."""

from pathlib import Path
import runpy
import unittest
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
CONVERT = runpy.run_path(str(ROOT / "art/app-icon/android_vector.py"))["foreground_vector"]
ANDROID = "{http://schemas.android.com/apk/res/android}"
AAPT = "{http://schemas.android.com/aapt}"
SHAPE = "M0 0L10 0L10 10Z"


def convert(content, *, definitions="", attributes="", view_box="0 0 100 100"):
    return CONVERT(ET.fromstring(
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{view_box}" {attributes}>'
        f'<defs>{definitions}</defs><g id="foreground">{content}</g></svg>'
    ))


def attributes(element):
    return {key.removeprefix(ANDROID): value for key, value in element.attrib.items()}


class AndroidVectorTest(unittest.TestCase):
    def test_nested_transforms_preserve_order_pivot_and_viewbox_origin(self):
        vector = convert(
            f'<g transform="translate(5, 6) scale(2 3)">'
            f'<path transform="rotate(90 4 8)" d="{SHAPE}"/></g>',
            view_box="10 20 100 100",
        )
        self.assertEqual(attributes(vector), {
            "width": "108dp", "height": "108dp", "viewportWidth": "108", "viewportHeight": "108",
        })
        chain = []
        node = vector
        while node.tag != "path":
            self.assertEqual(len(node), 1)
            node = node[0]
            if node.tag == "group" and node.attrib:
                chain.append(attributes(node))
        self.assertEqual(chain, [
            {"translateX": "18", "translateY": "18"},
            {"scaleX": "0.72", "scaleY": "0.72"},
            {"translateX": "-10", "translateY": "-20"},
            {"translateX": "5", "translateY": "6"},
            {"scaleX": "2", "scaleY": "3"},
            {"rotation": "90", "pivotX": "4", "pivotY": "8"},
        ])
        self.assertEqual(node.get(ANDROID + "pathData"), SHAPE)

    def test_styles_inherit_and_child_overrides_do_not_leak_to_siblings(self):
        vector = convert(
            f'<g opacity="0.5" fill-opacity="0.8" stroke-opacity="0.6" '
            f'stroke-width="3" stroke-linecap="round" stroke-linejoin="bevel" stroke-miterlimit="4">'
            f'<path d="{SHAPE}" opacity="0.5" fill="#ABCDEF" fill-opacity="0.2" fill-rule="evenodd"/>'
            f'<path d="{SHAPE}"/></g>',
            attributes='fill="#112233" stroke="#445566"',
        )
        child, sibling = [attributes(path) for path in vector.iter("path")]
        self.assertEqual(child["fillColor"], "#ABCDEF")
        self.assertAlmostEqual(float(child["fillAlpha"]), 0.05)
        self.assertAlmostEqual(float(child["strokeAlpha"]), 0.15)
        self.assertEqual(child["fillType"], "evenOdd")
        self.assertEqual(sibling["fillColor"], "#112233")
        self.assertAlmostEqual(float(sibling["fillAlpha"]), 0.4)
        self.assertAlmostEqual(float(sibling["strokeAlpha"]), 0.3)
        self.assertEqual(sibling["fillType"], "nonZero")
        for path in (child, sibling):
            for key, value in {"strokeColor": "#445566", "strokeWidth": "3",
                               "strokeLineCap": "round", "strokeLineJoin": "bevel",
                               "strokeMiterLimit": "4"}.items():
                self.assertEqual(path[key], value)

    def test_gradient_coordinates_stop_alpha_and_percentage_offsets(self):
        vector = convert(f'<path d="{SHAPE}" fill="url(#paper)"/>', definitions='''
            <linearGradient id="paper" gradientUnits="userSpaceOnUse" x1="2" y1="3" x2="40" y2="50">
                <stop offset="0%" stop-color="#123456" stop-opacity="0.5"/>
                <stop offset="25%" stop-color="#ABCDEF"/>
                <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
            </linearGradient>''')
        paint = next(vector.iter("path")).find(AAPT + "attr")
        self.assertEqual(paint.get("name"), "android:fillColor")
        gradient = paint.find("gradient")
        self.assertEqual(attributes(gradient), {
            "type": "linear", "startX": "2", "startY": "3", "endX": "40", "endY": "50",
        })
        self.assertEqual([attributes(stop) for stop in gradient], [
            {"offset": "0", "color": "#80123456"},
            {"offset": "0.25", "color": "#FFABCDEF"},
            {"offset": "1", "color": "#00FFFFFF"},
        ])

    def test_clip_precedes_its_artwork_and_does_not_clip_next_sibling(self):
        vector = convert(
            f'<g clip-path="url(#outline)"><path d="{SHAPE}"/></g>'
            '<path d="M20 20L30 30"/>',
            definitions='<clipPath id="outline"><path d="M1 1L9 1L9 9Z"/></clipPath>',
        )
        clip = next(vector.iter("clip-path"))
        scope = next(group for group in vector.iter("group") if clip in list(group))
        self.assertIs(scope[0], clip)
        self.assertEqual(clip.get(ANDROID + "pathData"), "M1 1L9 1L9 9Z")
        self.assertEqual([p.get(ANDROID + "pathData") for p in scope.iter("path")], [SHAPE])
        parent = next(group for group in vector.iter("group") if scope in list(group))
        self.assertEqual(parent[-1].get(ANDROID + "pathData"), "M20 20L30 30")

    def test_paint_order_preserves_default_and_places_stroke_before_fill(self):
        for order in (None, "normal", "stroke"):
            with self.subTest(order=order):
                paint_order = f'paint-order="{order}"' if order else ""
                vector = convert(f'<path d="{SHAPE}" fill="#112233" stroke="#445566" '
                                 f'stroke-width="2" {paint_order}/>')
                paths = [attributes(path) for path in vector.iter("path")]
                if order != "stroke":
                    self.assertEqual(len(paths), 1)
                    self.assertEqual(paths[0]["fillColor"], "#112233")
                    self.assertEqual(paths[0]["strokeColor"], "#445566")
                else:
                    self.assertEqual(len(paths), 2)
                    self.assertEqual(paths[0]["strokeColor"], "#445566")
                    self.assertNotIn("fillColor", paths[0])
                    self.assertEqual(paths[1]["fillColor"], "#112233")
                    self.assertNotIn("strokeColor", paths[1])
                self.assertTrue(all(path["pathData"] == SHAPE for path in paths))

    def test_shipped_foregrounds_convert_without_unsupported_features(self):
        for name in ("glass-v", "match-ticket", "arcade", "midnight", "mint"):
            with self.subTest(name=name):
                source = ET.parse(ROOT / f"art/app-icon/alternates/{name}.svg").getroot()
                self.assertGreater(len(list(CONVERT(source).iter("path"))), 0)

    def test_unsupported_artwork_fails_instead_of_silently_changing_rendering(self):
        cases = [
            ('<circle r="10"/>', "", "Unsupported foreground SVG element"),
            (f'<path d="{SHAPE}" transform="skewX(20)"/>', "", "Unsupported SVG transform"),
            (f'<path d="{SHAPE}" filter="url(#blur)"/>', "", "Unsupported path attributes"),
            (f'<path d="{SHAPE}" paint-order="markers"/>', "", "Unsupported paint order"),
            (f'<path d="{SHAPE}" fill="red"/>', "", "Unsupported SVG color"),
            (f'<path d="{SHAPE}" fill="url(#g)"/>',
             '<linearGradient id="g"/>', "Unsupported foreground gradient"),
            (f'<path d="{SHAPE}" fill="url(#g)"/>',
             '<linearGradient id="g" gradientUnits="userSpaceOnUse" gradientTransform="scale(2)"/>',
             "Unsupported gradient attributes"),
            (f'<path d="{SHAPE}" clip-path="url(#c)"/>',
             '<clipPath id="c"><rect width="10" height="10"/></clipPath>', "Unsupported clip path"),
        ]
        for content, definitions, message in cases:
            with self.subTest(content=content, definitions=definitions):
                with self.assertRaisesRegex(ValueError, message):
                    convert(content, definitions=definitions)
        for view_box in ("0 0 100 50", "0 0 0 0", "0 0 -1 -1"):
            with self.subTest(view_box=view_box):
                with self.assertRaisesRegex(ValueError, "square viewBox"):
                    convert(f'<path d="{SHAPE}"/>', view_box=view_box)


if __name__ == "__main__":
    unittest.main()
