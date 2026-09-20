"""Export the shipped SVG icon foregrounds to Android VectorDrawable XML."""

import re
import xml.etree.ElementTree as ET

ANDROID = "http://schemas.android.com/apk/res/android"
AAPT = "http://schemas.android.com/aapt"
ET.register_namespace("android", ANDROID)
ET.register_namespace("aapt", AAPT)


def _android(name: str) -> str:
    return f"{{{ANDROID}}}{name}"


def _tag(element: ET.Element) -> str:
    return element.tag.rsplit("}", 1)[-1]


def _number(value: float) -> str:
    return f"{value:g}"


def _transform(parent: ET.Element, value: str) -> ET.Element:
    remaining = value
    while remaining.strip():
        match = re.match(r"\s*(translate|scale|rotate)\(([^)]*)\)\s*,?", remaining)
        if match is None:
            raise ValueError(f"Unsupported SVG transform: {remaining}")
        operation, arguments = match.groups()
        numbers = [float(part) for part in re.split(r"[\s,]+", arguments.strip())]
        attrs = {}
        if operation == "translate" and len(numbers) in (1, 2):
            attrs = {"translateX": numbers[0], "translateY": numbers[1] if len(numbers) == 2 else 0}
        elif operation == "scale" and len(numbers) in (1, 2):
            attrs = {"scaleX": numbers[0], "scaleY": numbers[-1]}
        elif operation == "rotate" and len(numbers) in (1, 3):
            attrs = {"rotation": numbers[0]}
            if len(numbers) == 3:
                attrs.update(pivotX=numbers[1], pivotY=numbers[2])
        else:
            raise ValueError(f"Unsupported SVG transform arguments: {match.group(0)}")
        parent = ET.SubElement(parent, "group", {_android(key): _number(number) for key, number in attrs.items()})
        remaining = remaining[match.end():]
    return parent


def _paint(path: ET.Element, name: str, value: str, definitions: dict[str, ET.Element]) -> None:
    if value == "none":
        return
    reference = re.fullmatch(r"url\(#([^)]*)\)", value)
    if reference is None:
        if re.fullmatch(r"#[0-9a-fA-F]{6}", value) is None:
            raise ValueError(f"Unsupported SVG color: {value}")
        path.set(_android(name), value)
        return
    source = definitions[reference.group(1)]
    if _tag(source) != "linearGradient" or source.get("gradientUnits") != "userSpaceOnUse":
        raise ValueError(f"Unsupported foreground gradient: {reference.group(1)}")
    unsupported = set(source.attrib) - {"id", "x1", "y1", "x2", "y2", "gradientUnits"}
    if unsupported:
        raise ValueError(f"Unsupported gradient attributes: {unsupported}")
    attr = ET.SubElement(path, f"{{{AAPT}}}attr", {"name": f"android:{name}"})
    gradient = ET.SubElement(attr, "gradient", {
        _android("type"): "linear",
        **{_android(target): source.get(key, "0") for key, target in (
            ("x1", "startX"), ("y1", "startY"), ("x2", "endX"), ("y2", "endY"),
        )},
    })
    for stop in source:
        if _tag(stop) != "stop" or set(stop.attrib) - {"offset", "stop-color", "stop-opacity"}:
            raise ValueError("Unsupported SVG gradient stop")
        color = stop.attrib["stop-color"]
        if re.fullmatch(r"#[0-9a-fA-F]{6}", color) is None:
            raise ValueError(f"Unsupported gradient color: {color}")
        alpha = round(float(stop.get("stop-opacity", "1")) * 255)
        offset = stop.get("offset", "0")
        if offset.endswith("%"):
            offset = _number(float(offset[:-1]) / 100)
        ET.SubElement(gradient, "item", {
            _android("offset"): offset,
            _android("color"): f"#{alpha:02X}{color[1:]}",
        })


def foreground_vector(source: ET.Element) -> ET.Element:
    """Fit SVG foreground artwork into the central 72dp of a 108dp layer."""
    definitions = {element.attrib["id"]: element for element in source.iter() if "id" in element.attrib}
    foreground = definitions["foreground"]
    x, y, width, height = map(float, source.attrib["viewBox"].split())
    if width != height or width <= 0:
        raise ValueError("Launcher SVG must have a square viewBox")
    vector = ET.Element("vector", {
        _android("width"): "108dp", _android("height"): "108dp",
        _android("viewportWidth"): "108", _android("viewportHeight"): "108",
    })
    container = _transform(vector, f"translate(18 18) scale({72 / width}) translate({-x} {-y})")
    style_keys = {"fill", "stroke", "fill-opacity", "stroke-opacity", "stroke-width", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit", "fill-rule", "paint-order"}

    def visit(element: ET.Element, parent: ET.Element, inherited: dict[str, str], opacity: float) -> None:
        tag = _tag(element)
        if tag == "ellipse" and element.get("fill") == "url(#ground)":
            return
        if tag not in {"g", "path"}:
            raise ValueError(f"Unsupported foreground SVG element: {tag}")
        allowed = style_keys | {"id", "transform", "opacity", "shape-rendering", "clip-path"}
        if tag == "path":
            allowed.add("d")
        unsupported = set(element.attrib) - allowed
        if unsupported:
            raise ValueError(f"Unsupported {tag} attributes: {unsupported}")
        style = {**inherited, **{key: value for key, value in element.attrib.items() if key in style_keys}}
        opacity *= float(element.get("opacity", "1"))
        parent = _transform(parent, element.get("transform", ""))
        clip = element.get("clip-path")
        if clip:
            reference = re.fullmatch(r"url\(#([^)]*)\)", clip)
            if reference is None:
                raise ValueError(f"Unsupported clip: {clip}")
            clip_source = definitions[reference.group(1)]
            if _tag(clip_source) != "clipPath" or len(clip_source) != 1 or set(clip_source.attrib) != {"id"}:
                raise ValueError("Only a single path clip is supported")
            clip_path = clip_source[0]
            if _tag(clip_path) != "path" or set(clip_path.attrib) != {"d"}:
                raise ValueError("Unsupported clip path")
            parent = ET.SubElement(parent, "group")
            ET.SubElement(parent, "clip-path", {_android("pathData"): clip_path.attrib["d"]})
        if tag == "g":
            parent = ET.SubElement(parent, "group")
            for child in element:
                visit(child, parent, style, opacity)
            return
        paint_order = style.get("paint-order", "normal")
        if paint_order not in {"normal", "stroke"}:
            raise ValueError(f"Unsupported paint order: {paint_order}")
        paints = [("fill", "stroke")] if paint_order == "normal" else [("stroke",), ("fill",)]
        for paint_names in paints:
            path = ET.SubElement(parent, "path", {_android("pathData"): element.attrib["d"]})
            for paint_name in paint_names:
                _paint(path, f"{paint_name}Color", style.get(paint_name, "#000000" if paint_name == "fill" else "none"), definitions)
                path.set(_android(f"{paint_name}Alpha"), _number(opacity * float(style.get(f"{paint_name}-opacity", "1"))))
            for svg_key, android_key in (
                ("stroke-width", "strokeWidth"), ("stroke-linecap", "strokeLineCap"),
                ("stroke-linejoin", "strokeLineJoin"), ("stroke-miterlimit", "strokeMiterLimit"),
            ):
                if svg_key in style:
                    path.set(_android(android_key), style[svg_key])
            fill_rule = style.get("fill-rule", "nonzero")
            if fill_rule not in {"nonzero", "evenodd"}:
                raise ValueError(f"Unsupported fill rule: {fill_rule}")
            path.set(_android("fillType"), "evenOdd" if fill_rule == "evenodd" else "nonZero")

    visit(foreground, container, {key: value for key, value in source.attrib.items() if key in style_keys}, 1)
    return vector
