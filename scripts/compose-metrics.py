#!/usr/bin/env python3
"""Summarize and compare Compose compiler reports for VLR Compose modules."""

from __future__ import annotations

import argparse
import csv
import json
import re
import shutil
import subprocess
import sys
from collections import Counter
from pathlib import Path
from typing import Any


FEATURE_MODULES = (
    "feature-about",
    "feature-events",
    "feature-home",
    "feature-matches",
    "feature-news",
    "feature-player",
    "feature-rankings",
    "feature-team",
)
NUMERIC_METRICS = (
    "totalComposables",
    "restartableComposables",
    "skippableComposables",
    "readonlyComposables",
    "restartGroups",
    "totalGroups",
    "knownStableArguments",
    "knownUnstableArguments",
    "unknownStableArguments",
    "totalArguments",
    "markedStableClasses",
    "inferredStableClasses",
    "inferredUnstableClasses",
    "inferredUncertainClasses",
    "effectivelyStableClasses",
    "totalClasses",
)
PARAMETER_RE = re.compile(r"^\s{2}(stable|unstable|runtime|uncertain)\s+([^:]+):\s*(.*)$")
UNCLASSIFIED_PARAMETER_RE = re.compile(r"^\s{2}([A-Za-z_]\w*):\s*(.*)$")
CLASS_RE = re.compile(r"^(stable|unstable|runtime|uncertain)\s+class\s+([^\s{]+)")
FUNCTION_NAME_RE = re.compile(
    r"\bfun\s+(?:<[^>]*>\s*)?(?:[^\s(]+\.)?(`[^`]+`|[A-Za-z_]\w*)\s*\(",
    re.DOTALL,
)
DECISION_NODE_KINDS = (
    "if_expression",
    "when_entry",
    "for_statement",
    "while_statement",
    "do_while_statement",
    "catch_block",
    "conjunction_expression",
    "disjunction_expression",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)

    snapshot = commands.add_parser("snapshot", help="Create one deterministic metrics snapshot")
    snapshot.add_argument("--modules", nargs="+", default=FEATURE_MODULES, help="Modules to measure")
    snapshot.add_argument("--raw", type=Path, required=True, help="Compiler report root")
    snapshot.add_argument("--source-root", type=Path, required=True, help="Repository or extracted source root")
    snapshot.add_argument("--source-label", help="Stable provenance label for the source input")
    snapshot.add_argument("--output", type=Path, required=True, help="Snapshot JSON path")
    snapshot.add_argument("--markdown", type=Path, help="Optional human-readable summary")
    snapshot.add_argument("--revision", required=True, help="Git revision represented by the inputs")
    snapshot.add_argument("--build-command", required=True, help="Exact compiler report command")

    compare = commands.add_parser("compare", help="Compare two snapshot JSON files")
    compare.add_argument("baseline", type=Path)
    compare.add_argument("current", type=Path)
    compare.add_argument("--output", type=Path, required=True, help="Comparison JSON path")
    compare.add_argument("--markdown", type=Path, help="Optional human-readable comparison")
    return parser.parse_args()


def one_file(root: Path, pattern: str) -> Path:
    matches = sorted(root.glob(pattern))
    if len(matches) != 1:
        raise ValueError(f"expected one {pattern} below {root}, found {len(matches)}")
    return matches[0]


def read_json(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as stream:
        return json.load(stream)


def parse_parameters(path: Path) -> dict[str, Any]:
    counts: Counter[str] = Counter()
    non_stable: list[dict[str, str]] = []
    unclassified: list[dict[str, str]] = []
    current_function = "<unknown>"
    for line in path.read_text(encoding="utf-8").splitlines():
        if " fun " in f" {line}" or line.startswith("fun "):
            match = re.search(r"\bfun\s+([^\s(]+)\s*\(", line)
            if match:
                current_function = match.group(1)
        match = PARAMETER_RE.match(line)
        if not match:
            unclassified_match = UNCLASSIFIED_PARAMETER_RE.match(line)
            if unclassified_match:
                name, value_type = unclassified_match.groups()
                counts["unclassified"] += 1
                unclassified.append(
                    {
                        "function": current_function,
                        "name": name.strip(),
                        "type": value_type.strip(),
                    }
                )
            continue
        stability, name, value_type = match.groups()
        counts[stability] += 1
        if stability != "stable":
            non_stable.append(
                {
                    "function": current_function,
                    "name": name.strip(),
                    "type": value_type.strip(),
                    "stability": stability,
                }
            )
    return {
        "counts": dict(sorted(counts.items())),
        "nonStable": non_stable,
        "unclassified": unclassified,
    }


def parse_classes(path: Path) -> dict[str, Any]:
    counts: Counter[str] = Counter()
    names: dict[str, list[str]] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        match = CLASS_RE.match(line)
        if not match:
            continue
        stability, name = match.groups()
        counts[stability] += 1
        names.setdefault(stability, []).append(name)
    return {
        "counts": dict(sorted(counts.items())),
        "names": {key: sorted(value) for key, value in sorted(names.items())},
    }


def parse_composables_csv(path: Path) -> dict[str, Any]:
    with path.open(newline="", encoding="utf-8") as stream:
        rows = list(csv.DictReader(stream))
    by_qualified_name: dict[str, dict[str, Any]] = {}
    name_counts = Counter(row["package"] for row in rows if row.get("composable") == "1")
    overload_counts: Counter[str] = Counter()
    for row in rows:
        if row.get("composable") != "1":
            continue
        qualified_name = row["package"]
        if name_counts[qualified_name] > 1:
            overload_counts[qualified_name] += 1
            qualified_name += f"#overload-{overload_counts[qualified_name]}"
        by_qualified_name[qualified_name] = {
            "name": row["name"],
            "eligibleForSkipping": row.get("skippable") == "1",
            "restartable": row.get("restartable") == "1",
            "readonly": row.get("readonly") == "1",
            "inline": row.get("inline") == "1",
            "groups": int(row.get("groups") or 0),
            "calls": int(row.get("calls") or 0),
        }
    eligible = sum(1 for value in by_qualified_name.values() if value["eligibleForSkipping"])
    return {
        "namedComposableCount": len(by_qualified_name),
        "eligibleForSkippingCount": eligible,
        "notEligibleForSkippingCount": len(by_qualified_name) - eligible,
        "byQualifiedName": dict(sorted(by_qualified_name.items())),
    }


def source_set_for(path: str) -> str:
    match = re.search(r"/src/([^/]+)/", f"/{path}")
    return match.group(1) if match else "unknown"


def source_functions(source_root: Path, module: str) -> dict[str, Any]:
    ast_grep = shutil.which("ast-grep")
    if not ast_grep:
        raise RuntimeError("ast-grep is required for Kotlin function boundary parsing")
    module_root = source_root / module
    files = sorted(str(path) for path in module_root.glob("src/*Main/**/*.kt"))
    if not files:
        return function_summary([])
    def ast_nodes(kind: str) -> list[dict[str, Any]]:
        result = subprocess.run(
            [ast_grep, "run", "--lang", "kotlin", "--kind", kind, "--json=stream", *files],
            cwd=source_root,
            check=False,
            capture_output=True,
            text=True,
        )
        if result.returncode not in (0, 1):
            raise subprocess.CalledProcessError(result.returncode, result.args, result.stdout, result.stderr)
        return [json.loads(line) for line in result.stdout.splitlines()]

    functions: list[dict[str, Any]] = []
    for parsed in ast_nodes("function_declaration"):
        text = parsed["text"]
        name_match = FUNCTION_NAME_RE.search(text)
        if not name_match:
            continue
        start = parsed["range"]["start"]["line"] + 1
        end = parsed["range"]["end"]["line"] + 1
        path = str(Path(parsed["file"]).relative_to(source_root))
        functions.append(
            {
                "path": path,
                "sourceSet": source_set_for(path),
                "name": name_match.group(1),
                "startLine": start,
                "endLine": end,
                "lines": end - start + 1,
                "byteStart": parsed["range"]["byteOffset"]["start"],
                "byteEnd": parsed["range"]["byteOffset"]["end"],
                "complexityEstimate": 1,
                "composable": "@Composable" in text[: name_match.start()],
            }
        )
    for kind in DECISION_NODE_KINDS:
        for node in ast_nodes(kind):
            if kind == "when_entry" and node["text"].lstrip().startswith("else"):
                continue
            node_path = str(Path(node["file"]).relative_to(source_root))
            node_start = node["range"]["byteOffset"]["start"]
            node_end = node["range"]["byteOffset"]["end"]
            owners = [
                function
                for function in functions
                if function["path"] == node_path
                and function["byteStart"] <= node_start
                and node_end <= function["byteEnd"]
            ]
            if owners:
                min(owners, key=lambda function: function["byteEnd"] - function["byteStart"])["complexityEstimate"] += 1
    for function in functions:
        del function["byteStart"]
        del function["byteEnd"]
    functions.sort(key=lambda item: (item["path"], item["startLine"], item["name"]))
    return function_summary(functions)


def function_summary(functions: list[dict[str, Any]]) -> dict[str, Any]:
    total_lines = sum(item["lines"] for item in functions)
    total_complexity = sum(item["complexityEstimate"] for item in functions)
    count = len(functions)
    return {
        "count": count,
        "composableCount": sum(1 for item in functions if item["composable"]),
        "totalLines": total_lines,
        "meanLines": round(total_lines / count, 3) if count else 0,
        "maxLines": max((item["lines"] for item in functions), default=0),
        "totalComplexityEstimate": total_complexity,
        "meanComplexityEstimate": round(total_complexity / count, 3) if count else 0,
        "maxComplexityEstimate": max((item["complexityEstimate"] for item in functions), default=0),
        "functions": functions,
    }


def aggregate(modules: dict[str, dict[str, Any]]) -> dict[str, Any]:
    compiler = {metric: sum(module["compiler"].get(metric, 0) for module in modules.values()) for metric in NUMERIC_METRICS}
    compiler["nonRestartableComposables"] = compiler["totalComposables"] - compiler["restartableComposables"]
    compiler["nonSkippableComposables"] = compiler["totalComposables"] - compiler["skippableComposables"]
    parameter_counts: Counter[str] = Counter()
    class_counts: Counter[str] = Counter()
    all_functions: list[dict[str, Any]] = []
    named_composables: dict[str, dict[str, Any]] = {}
    for module_name, module in modules.items():
        parameter_counts.update(module["reportedComposableParameters"]["counts"])
        class_counts.update(module["reportedClasses"]["counts"])
        all_functions.extend({"module": module_name, **item} for item in module["sourceFunctions"]["functions"])
        for qualified_name, details in module["reportedComposables"]["byQualifiedName"].items():
            if qualified_name in named_composables:
                raise ValueError(f"duplicate named composable across modules: {qualified_name}")
            named_composables[qualified_name] = {"module": module_name, **details}
    eligible = sum(1 for value in named_composables.values() if value["eligibleForSkipping"])
    return {
        "compiler": compiler,
        "reportedComposableParameters": dict(sorted(parameter_counts.items())),
        "reportedClasses": dict(sorted(class_counts.items())),
        "reportedComposables": {
            "namedComposableCount": len(named_composables),
            "eligibleForSkippingCount": eligible,
            "notEligibleForSkippingCount": len(named_composables) - eligible,
            "byQualifiedName": dict(sorted(named_composables.items())),
        },
        "sourceFunctions": function_summary(all_functions),
    }


def snapshot(args: argparse.Namespace) -> dict[str, Any]:
    modules: dict[str, dict[str, Any]] = {}
    feature_flags: dict[str, bool] | None = None
    for module in args.modules:
        module_root = args.raw / module
        compiler = read_json(one_file(module_root, "metrics/*/main/*-module.json"))
        flags = compiler.get("featureFlags", {})
        if feature_flags is None:
            feature_flags = flags
        elif flags != feature_flags:
            raise ValueError(f"compiler feature flags differ for {module}")
        compiler["nonRestartableComposables"] = compiler["totalComposables"] - compiler["restartableComposables"]
        compiler["nonSkippableComposables"] = compiler["totalComposables"] - compiler["skippableComposables"]
        modules[module] = {
            "compiler": compiler,
            "reportedComposableParameters": parse_parameters(one_file(module_root, "reports/*-composables.txt")),
            "reportedClasses": parse_classes(one_file(module_root, "reports/*-classes.txt")),
            "reportedComposables": parse_composables_csv(one_file(module_root, "reports/*-composables.csv")),
            "sourceFunctions": source_functions(args.source_root.resolve(), module),
        }
    result = {
        "schemaVersion": 1,
        "provenance": {
            "revision": args.revision,
            "buildCommand": args.build_command,
            "compilerReportsRoot": str(args.raw),
            "sourceInput": args.source_label or str(args.source_root),
            "compilerFeatureFlags": feature_flags,
            "composeMetricsGradleProperty": True,
        },
        "methodology": {
            "compilerCounts": "Kotlin Compose compiler module JSON",
            "parameterAndClassDetails": "Kotlin Compose compiler text reports",
            "namedComposableEligibility": "Compose compiler CSV named functions; eligible means the compiler marked skippable=1. JSON totalComposables also includes generated composables and lambdas.",
            "sourceFunctionBoundaries": "ast-grep 0.45+ Kotlin tree-sitter function_declaration nodes",
            "complexity": "AST estimate: 1 + if + non-else when entries + for/while/do-while + catch + conjunction/disjunction nodes; nested decisions belong to the innermost function",
            "functionLength": "Inclusive start-to-end lines from ast-grep ranges; annotations and signatures count",
        },
        "totals": aggregate(modules),
        "modules": modules,
    }
    return result


def diff_numbers(before: dict[str, Any], after: dict[str, Any], keys: tuple[str, ...]) -> dict[str, Any]:
    return {
        key: {"before": before.get(key, 0), "after": after.get(key, 0), "delta": after.get(key, 0) - before.get(key, 0)}
        for key in keys
    }


def compare_named_composables(before: dict[str, Any], after: dict[str, Any]) -> dict[str, Any]:
    before_names = before["byQualifiedName"]
    after_names = after["byQualifiedName"]
    # CSV has no parameter signatures, so overload ordinals cannot identify functions across builds.
    ambiguous = {
        name.split("#overload-", 1)[0]
        for name in set(before_names) | set(after_names)
        if "#overload-" in name
    }
    before_names = {name: value for name, value in before_names.items() if name.split("#overload-", 1)[0] not in ambiguous}
    after_names = {name: value for name, value in after_names.items() if name.split("#overload-", 1)[0] not in ambiguous}
    matched = sorted(set(before_names) & set(after_names))
    transitions = [
        {
            "qualifiedName": name,
            "before": before_names[name],
            "after": after_names[name],
        }
        for name in matched
        if before_names[name] != after_names[name]
    ]
    eligibility_transitions = [
        transition
        for transition in transitions
        if transition["before"]["eligibleForSkipping"] != transition["after"]["eligibleForSkipping"]
    ]
    return {
        "counts": diff_numbers(
            before,
            after,
            ("namedComposableCount", "eligibleForSkippingCount", "notEligibleForSkippingCount"),
        ),
        "overloadGroupsExcludedFromIdentityComparison": sorted(ambiguous),
        "matchedCount": len(matched),
        "added": sorted(set(after_names) - set(before_names)),
        "removed": sorted(set(before_names) - set(after_names)),
        "attributeTransitions": transitions,
        "eligibilityTransitions": eligibility_transitions,
    }


def compare(args: argparse.Namespace) -> dict[str, Any]:
    baseline = read_json(args.baseline)
    current = read_json(args.current)
    compiler_keys = NUMERIC_METRICS + ("nonRestartableComposables", "nonSkippableComposables")
    source_keys = (
        "count",
        "composableCount",
        "totalLines",
        "meanLines",
        "maxLines",
        "totalComplexityEstimate",
        "meanComplexityEstimate",
        "maxComplexityEstimate",
    )
    modules: dict[str, Any] = {}
    if set(baseline["modules"]) != set(current["modules"]):
        raise ValueError("baseline and current must contain the same modules")
    for module in baseline["modules"]:
        before = baseline["modules"][module]
        after = current["modules"][module]
        modules[module] = {
            "compiler": diff_numbers(before["compiler"], after["compiler"], compiler_keys),
            "reportedComposableParameters": diff_numbers(
                before["reportedComposableParameters"]["counts"],
                after["reportedComposableParameters"]["counts"],
                tuple(sorted(set(before["reportedComposableParameters"]["counts"]) | set(after["reportedComposableParameters"]["counts"]))),
            ),
            "reportedClasses": diff_numbers(
                before["reportedClasses"]["counts"],
                after["reportedClasses"]["counts"],
                tuple(sorted(set(before["reportedClasses"]["counts"]) | set(after["reportedClasses"]["counts"]))),
            ),
            "reportedComposables": compare_named_composables(before["reportedComposables"], after["reportedComposables"]),
            "sourceFunctions": diff_numbers(before["sourceFunctions"], after["sourceFunctions"], source_keys),
        }
    return {
        "schemaVersion": 1,
        "baselineRevision": baseline["provenance"]["revision"],
        "currentRevision": current["provenance"]["revision"],
        "compilerFeatureFlagsMatch": baseline["provenance"]["compilerFeatureFlags"] == current["provenance"]["compilerFeatureFlags"],
        "totals": {
            "compiler": diff_numbers(baseline["totals"]["compiler"], current["totals"]["compiler"], compiler_keys),
            "reportedComposableParameters": diff_numbers(
                baseline["totals"]["reportedComposableParameters"],
                current["totals"]["reportedComposableParameters"],
                tuple(
                    sorted(
                        set(baseline["totals"]["reportedComposableParameters"])
                        | set(current["totals"]["reportedComposableParameters"])
                    )
                ),
            ),
            "reportedClasses": diff_numbers(
                baseline["totals"]["reportedClasses"],
                current["totals"]["reportedClasses"],
                tuple(sorted(set(baseline["totals"]["reportedClasses"]) | set(current["totals"]["reportedClasses"]))),
            ),
            "reportedComposables": compare_named_composables(
                baseline["totals"]["reportedComposables"],
                current["totals"]["reportedComposables"],
            ),
            "sourceFunctions": diff_numbers(baseline["totals"]["sourceFunctions"], current["totals"]["sourceFunctions"], source_keys),
        },
        "modules": modules,
    }


def write_json(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def snapshot_markdown(value: dict[str, Any]) -> str:
    lines = [
        "# Compose metrics snapshot",
        "",
        f"Revision: `{value['provenance']['revision']}`",
        "",
        "| Module | Compiler total | Restartable | Non-restartable | Skippable | Non-skippable | Stable args | Unstable args | Unclassified reported params | Stable classes | Unstable classes | Functions | Mean lines | Max lines | Mean complexity estimate | Max complexity estimate |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for name, module in value["modules"].items():
        compiler = module["compiler"]
        parameters = module["reportedComposableParameters"]["counts"]
        source = module["sourceFunctions"]
        lines.append(
            f"| {name} | {compiler['totalComposables']} | {compiler['restartableComposables']} | {compiler['nonRestartableComposables']} | "
            f"{compiler['skippableComposables']} | {compiler['nonSkippableComposables']} | {compiler['knownStableArguments']} | "
            f"{compiler['knownUnstableArguments']} | {parameters.get('unclassified', 0)} | {compiler['effectivelyStableClasses']} | {compiler['inferredUnstableClasses']} | "
            f"{source['count']} | {source['meanLines']} | {source['maxLines']} | {source['meanComplexityEstimate']} | {source['maxComplexityEstimate']} |"
        )
    total = value["totals"]
    compiler = total["compiler"]
    parameters = total["reportedComposableParameters"]
    source = total["sourceFunctions"]
    lines.append(
        f"| **Total** | **{compiler['totalComposables']}** | **{compiler['restartableComposables']}** | **{compiler['nonRestartableComposables']}** | "
        f"**{compiler['skippableComposables']}** | **{compiler['nonSkippableComposables']}** | **{compiler['knownStableArguments']}** | "
        f"**{compiler['knownUnstableArguments']}** | **{parameters.get('unclassified', 0)}** | **{compiler['effectivelyStableClasses']}** | **{compiler['inferredUnstableClasses']}** | "
        f"**{source['count']}** | **{source['meanLines']}** | **{source['maxLines']}** | **{source['meanComplexityEstimate']}** | **{source['maxComplexityEstimate']}** |"
    )
    lines.extend(
        [
            "",
            "Compiler total includes generated composables and composable lambdas.",
            "",
            "| Module | Named composables | Eligible for skipping | Not eligible for skipping |",
            "|---|---:|---:|---:|",
        ]
    )
    for name, module in value["modules"].items():
        named = module["reportedComposables"]
        lines.append(
            f"| {name} | {named['namedComposableCount']} | {named['eligibleForSkippingCount']} | {named['notEligibleForSkippingCount']} |"
        )
    named = value["totals"]["reportedComposables"]
    lines.extend(
        [
            f"| **Total** | **{named['namedComposableCount']}** | **{named['eligibleForSkippingCount']}** | **{named['notEligibleForSkippingCount']}** |",
            "",
            "Complexity is an AST-based estimate. The Kotlin parser supplies function and decision nodes. See the JSON methodology field for the exact formula.",
            "",
            "Compiler feature flags: `" + json.dumps(value["provenance"]["compilerFeatureFlags"], sort_keys=True) + "`",
            "",
        ]
    )
    return "\n".join(lines)


def compare_markdown(value: dict[str, Any]) -> str:
    lines = [
        "# Compose metrics comparison",
        "",
        f"Baseline: `{value['baselineRevision']}`  ",
        f"Current: `{value['currentRevision']}`",
        "",
        "| Module | Compiler total | Restartable | Skippable | Non-skippable | Named | Eligible | Not eligible | Added | Removed | Eligibility transitions | Stable args | Unstable args | Unclassified params | Stable classes | Unstable classes | Functions | Mean lines | Max lines | Mean complexity* | Max complexity* |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for name, module in value["modules"].items():
        compiler = module["compiler"]
        parameters = module["reportedComposableParameters"]
        named = module["reportedComposables"]
        source = module["sourceFunctions"]
        lines.append(
            f"| {name} | {compiler['totalComposables']['delta']:+g} | {compiler['restartableComposables']['delta']:+g} | "
            f"{compiler['skippableComposables']['delta']:+g} | {compiler['nonSkippableComposables']['delta']:+g} | "
            f"{named['counts']['namedComposableCount']['delta']:+g} | {named['counts']['eligibleForSkippingCount']['delta']:+g} | "
            f"{named['counts']['notEligibleForSkippingCount']['delta']:+g} | {len(named['added'])} | {len(named['removed'])} | "
            f"{len(named['eligibilityTransitions'])} | "
            f"{compiler['knownStableArguments']['delta']:+g} | {compiler['knownUnstableArguments']['delta']:+g} | "
            f"{parameters.get('unclassified', {'delta': 0})['delta']:+g} | "
            f"{compiler['effectivelyStableClasses']['delta']:+g} | {compiler['inferredUnstableClasses']['delta']:+g} | "
            f"{source['count']['delta']:+g} | {source['meanLines']['delta']:+g} | {source['maxLines']['delta']:+g} | "
            f"{source['meanComplexityEstimate']['delta']:+g} | {source['maxComplexityEstimate']['delta']:+g} |"
        )
    lines.extend(
        [
            "",
            f"Compiler feature flags match: `{str(value['compilerFeatureFlagsMatch']).lower()}`",
            "",
            "*Complexity is the AST-based estimate recorded in each snapshot.",
            "",
        ]
    )
    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    try:
        if args.command == "snapshot":
            value = snapshot(args)
            markdown = snapshot_markdown(value)
        else:
            value = compare(args)
            markdown = compare_markdown(value)
        write_json(args.output, value)
        if args.markdown:
            args.markdown.parent.mkdir(parents=True, exist_ok=True)
            args.markdown.write_text(markdown, encoding="utf-8")
    except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"compose-metrics: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
