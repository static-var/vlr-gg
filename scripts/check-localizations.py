#!/usr/bin/env python3
"""Check supported-language coverage and formatting arguments in shipped resources."""

import argparse
from collections import Counter
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


LANGUAGES = ("pt-rBR", "hi", "tr", "es", "fr", "de", "ko", "ru")
FORMAT = re.compile(r"%(?:(\d+)\$)?[-+#0,]*(?:\d+|\*)?(?:\.\d+)?(?:hh|ll|[hlLzjt])?([@a-zA-Z%])")
APPLE_STRING = re.compile(r'^\s*"((?:\\.|[^"\\])*)"\s*=\s*"((?:\\.|[^"\\])*)"\s*;\s*$')


def arguments(text):
    result = Counter()
    position = 0
    for match in FORMAT.finditer(text):
        index, kind = match.groups()
        if kind == "%":
            continue
        position += 1
        result[(int(index) if index else position, kind)] += 1
    return result


def xml_strings(path):
    strings = {}
    for element in ET.parse(path).getroot():
        if element.tag != "string" or element.get("translatable") == "false":
            continue
        key = element.attrib["name"]
        if key in strings:
            raise ValueError(f"{path}: duplicate key {key}")
        strings[key] = "".join(element.itertext())
    return strings


def xml_plurals(path):
    result = {}
    for element in ET.parse(path).getroot():
        if element.tag != "plurals" or element.get("translatable") == "false":
            continue
        key = element.attrib["name"]
        if key in result:
            raise ValueError(f"{path}: duplicate plural {key}")
        forms = {}
        for item in element:
            quantity = item.attrib["quantity"]
            if quantity in forms:
                raise ValueError(f"{path}: duplicate quantity for {key}")
            forms[quantity] = "".join(item.itertext())
        if "other" not in forms:
            raise ValueError(f"{path}: plural {key} has no other form")
        result[key] = forms
    return result


def check_xml(root):
    errors = []
    count = 0
    bases = list(root.glob("*/src/commonMain/composeResources/values/*.xml"))
    bases += [root / "androidApp/src/main/res/values/strings.xml"]
    for base in sorted(bases):
        source = xml_strings(base)
        source_plurals = xml_plurals(base)
        for language in LANGUAGES:
            target = base.parent.parent / f"values-{language}" / base.name
            if not target.exists():
                errors.append(f"{target.relative_to(root)}: missing translation file")
                continue
            translated = xml_strings(target)
            for key in source.keys() - translated.keys():
                errors.append(f"{target.relative_to(root)}: missing {key}")
            for key in translated.keys() - source.keys():
                errors.append(f"{target.relative_to(root)}: unknown {key}")
            for key in source.keys() & translated.keys():
                count += 1
                if not translated[key].strip():
                    errors.append(f"{target.relative_to(root)}: empty {key}")
                if arguments(source[key]) != arguments(translated[key]):
                    errors.append(f"{target.relative_to(root)}: format arguments differ for {key}")
            translated_plurals = xml_plurals(target)
            for key in source_plurals.keys() - translated_plurals.keys():
                errors.append(f"{target.relative_to(root)}: missing plural {key}")
            for key in translated_plurals.keys() - source_plurals.keys():
                errors.append(f"{target.relative_to(root)}: unknown plural {key}")
            required = {"one", "few", "many", "other"} if language == "ru" else {"other"}
            for key in source_plurals.keys() & translated_plurals.keys():
                forms = translated_plurals[key]
                for quantity in required - forms.keys():
                    errors.append(f"{target.relative_to(root)}: missing {quantity} form for {key}")
                for quantity, value in forms.items():
                    count += 1
                    expected = source_plurals[key].get(quantity, source_plurals[key]["other"])
                    if not value.strip() or arguments(expected) != arguments(value):
                        errors.append(f"{target.relative_to(root)}: invalid {quantity} form for {key}")
    return errors, count


def check_catalogs(root):
    errors = []
    count = 0
    languages = ("en",) + tuple(lang.replace("-r", "-") for lang in LANGUAGES)
    for path in sorted((root / "iosApp").rglob("*.xcstrings")):
        if "build" in path.parts:
            continue
        catalog = json.loads(path.read_text())
        for key, entry in catalog["strings"].items():
            if entry.get("shouldTranslate") is False:
                continue
            localizations = entry.get("localizations", {})
            source_units = dict(catalog_units(localizations.get("en", {})))
            for language in languages:
                localized = localizations.get(language, {})
                units = catalog_units(localized)
                if not units:
                    errors.append(f"{path.relative_to(root)}: missing {language} for {key}")
                    continue
                for plural in catalog_plurals(localized):
                    if language == "ru" and not {"one", "few", "many", "other"} <= plural.keys():
                        errors.append(f"{path.relative_to(root)}: missing Russian plural forms for {key}")
                for unit_path, unit in units:
                    count += 1
                    value = unit.get("value", "")
                    if not value.strip() or unit.get("state") != "translated":
                        errors.append(f"{path.relative_to(root)}: incomplete {language} for {key}")
                    source_unit = source_units.get(unit_path)
                    if source_unit is None and unit_path and unit_path[-1] in {"zero", "one", "two", "few", "many"}:
                        source_unit = source_units.get(unit_path[:-1] + ("other",))
                    source = source_unit.get("value", key) if source_unit else key
                    if arguments(source) != arguments(value):
                        errors.append(f"{path.relative_to(root)}: {language} format arguments differ for {key}")
                    if Counter(re.findall(r"\$\{[^}]+\}", source)) != Counter(re.findall(r"\$\{[^}]+\}", value)):
                        errors.append(f"{path.relative_to(root)}: {language} shortcut tokens differ for {key}")
    return errors, count


def catalog_units(value, path=()):
    if not isinstance(value, dict):
        return []
    result = []
    if "stringUnit" in value:
        result.append((path, value["stringUnit"]))
    for key, child in value.items():
        if key != "stringUnit":
            result.extend(catalog_units(child, path + (key,)))
    return result


def catalog_plurals(value):
    if not isinstance(value, dict):
        return []
    result = [value["plural"]] if isinstance(value.get("plural"), dict) else []
    for key, child in value.items():
        if key != "plural":
            result.extend(catalog_plurals(child))
    return result


def apple_strings(path):
    result = {}
    for line_number, line in enumerate(path.read_text().splitlines(), 1):
        stripped = line.strip()
        if not stripped or stripped.startswith("//") or stripped.startswith("/*") or stripped.startswith("*"):
            continue
        match = APPLE_STRING.fullmatch(line)
        if match is None:
            raise ValueError(f"{path}:{line_number}: invalid .strings entry")
        key, value = (json.loads(f'"{item}"') for item in match.groups())
        if key in result:
            raise ValueError(f"{path}:{line_number}: duplicate key {key}")
        result[key] = value
    return result


def check_native_strings(root):
    errors = []
    count = 0
    languages = ("en",) + tuple(lang.replace("-r", "-") for lang in LANGUAGES)
    for source in sorted((root / "iosApp").rglob("en.lproj/*.strings")):
        if "build" in source.parts:
            continue
        table = apple_strings(source)
        localized_root = source.parent.parent
        for language in languages:
            target = localized_root / f"{language}.lproj" / source.name
            if not target.exists():
                errors.append(f"{target.relative_to(root)}: missing native translation file")
                continue
            translated = apple_strings(target)
            for key in table.keys() - translated.keys():
                errors.append(f"{target.relative_to(root)}: missing {key}")
            for key in translated.keys() - table.keys():
                errors.append(f"{target.relative_to(root)}: unknown {key}")
            for key in table.keys() & translated.keys():
                count += 1
                value = translated[key]
                if not value.strip():
                    errors.append(f"{target.relative_to(root)}: empty {key}")
                if arguments(table[key]) != arguments(value):
                    errors.append(f"{target.relative_to(root)}: format arguments differ for {key}")
                if Counter(re.findall(r"\$\{[^}]+\}", table[key])) != Counter(re.findall(r"\$\{[^}]+\}", value)):
                    errors.append(f"{target.relative_to(root)}: shortcut tokens differ for {key}")
    return errors, count


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parent.parent)
    args = parser.parse_args()
    try:
        xml_errors, xml_count = check_xml(args.root)
        catalog_errors, catalog_count = check_catalogs(args.root)
        strings_errors, strings_count = check_native_strings(args.root)
    except (OSError, ValueError, ET.ParseError) as error:
        print(error, file=sys.stderr)
        return 1
    errors = xml_errors + catalog_errors + strings_errors
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Verified {xml_count} XML translations and {catalog_count + strings_count} native translations.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
