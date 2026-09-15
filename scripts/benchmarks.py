#!/usr/bin/env python3
"""Archive AndroidX benchmark JSON and compare two archived runs."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable


ARCHIVE_SCHEMA_VERSION = 1
DEFAULT_ARTIFACT_LIMIT = 512 * 1024 * 1024
TRACE_SUFFIXES = (".perfetto-trace", ".trace", ".dmtrace", ".stack")
SUMMARY_NAMES = ("summary.json", "summary.csv", "summary.md")


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)

    archive = commands.add_parser("archive", help="Archive one AndroidX benchmark run")
    archive.add_argument("input", type=Path, help="BenchmarkData JSON file or directory")
    archive.add_argument("--output-dir", type=Path, default=Path("benchmark-results"))
    archive.add_argument("--label", help="Short name appended to the UTC archive timestamp")
    archive.add_argument("--metadata", type=Path, help="Optional JSON with run conditions")
    archive.add_argument("--apk-metadata", type=Path, help="Optional APK output-metadata JSON")
    archive.add_argument("--artifacts", type=Path, help="Directory containing trace artifacts")
    archive.add_argument(
        "--max-artifact-bytes",
        type=int,
        default=DEFAULT_ARTIFACT_LIMIT,
        help=f"Maximum total trace bytes to copy (default: {DEFAULT_ARTIFACT_LIMIT})",
    )

    compare = commands.add_parser("compare", help="Compare two benchmark archives")
    compare.add_argument("baseline", type=Path)
    compare.add_argument("current", type=Path)
    compare.add_argument("--output-dir", type=Path, help="Comparison directory")
    return parser.parse_args(argv)


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"cannot read JSON from {path}: {error}") from error
    if not isinstance(value, dict):
        raise ValueError(f"expected a JSON object in {path}")
    return value


def write_json(path: Path, value: Any) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def is_androidx_benchmark_data(value: dict[str, Any]) -> bool:
    return isinstance(value.get("context"), dict) and isinstance(value.get("benchmarks"), list)


def discover_inputs(path: Path) -> list[tuple[Path, dict[str, Any]]]:
    if not path.exists():
        raise ValueError(f"input does not exist: {path}")
    candidates = [path] if path.is_file() else sorted(path.rglob("*.json"))
    parsed: list[tuple[Path, dict[str, Any]]] = []
    errors: list[str] = []
    for candidate in candidates:
        try:
            value = read_json(candidate)
        except ValueError as error:
            if path.is_file():
                raise
            errors.append(str(error))
            continue
        if is_androidx_benchmark_data(value):
            parsed.append((candidate, value))
        elif path.is_file():
            raise ValueError(f"{path} is not AndroidX BenchmarkData JSON")
    if not parsed:
        detail = f"; {errors[0]}" if errors else ""
        raise ValueError(f"found no AndroidX BenchmarkData JSON below {path}{detail}")
    return parsed


def portable_relative(path: Path, root: Path) -> str:
    if root.is_file():
        return path.name
    return path.relative_to(root).as_posix()


def portable_filename(value: Any) -> str | None:
    if not isinstance(value, str) or not value:
        return None
    candidate = Path(value)
    if candidate.is_absolute() or ".." in candidate.parts:
        return candidate.name
    return candidate.as_posix()


def number(value: Any) -> int | float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        return None
    return value


def numeric_list(value: Any) -> list[int | float]:
    if not isinstance(value, list):
        return []
    return [item for item in value if number(item) is not None]


def nested_numeric_lists(value: Any) -> list[list[int | float]]:
    if not isinstance(value, list):
        return []
    return [numeric_list(item) for item in value if isinstance(item, list)]


def normalize_context(context: dict[str, Any]) -> dict[str, Any]:
    build = context.get("build") if isinstance(context.get("build"), dict) else {}
    version = build.get("version") if isinstance(build.get("version"), dict) else {}
    return {
        "build": {
            key: build.get(key)
            for key in ("brand", "device", "fingerprint", "id", "model", "type")
            if build.get(key) is not None
        }
        | {
            "version": {
                key: version.get(key) for key in ("codename", "sdk") if version.get(key) is not None
            }
        },
        **{
            key: context.get(key)
            for key in (
                "cpuCoreCount",
                "cpuLocked",
                "cpuMaxFreqHz",
                "memTotalBytes",
                "sustainedPerformanceModeEnabled",
                "artMainlineVersion",
                "osCodenameAbbreviated",
                "compilationMode",
            )
            if context.get(key) is not None
        },
        **(
            {"payload": portable_metadata(context["payload"])}
            if context.get("payload") is not None
            else {}
        ),
        "emulator": is_emulator(build),
    }


def is_emulator(build: dict[str, Any]) -> bool:
    text = " ".join(str(build.get(key, "")) for key in ("brand", "device", "fingerprint", "model"))
    return any(token in text.lower() for token in ("generic", "emulator", "sdk_gphone", "ranchu", "goldfish"))


def infer_input_schema(value: dict[str, Any]) -> dict[str, Any]:
    benchmarks = value.get("benchmarks", [])
    benchmark_keys = sorted({key for item in benchmarks if isinstance(item, dict) for key in item})
    context = value.get("context", {})
    context_keys = sorted(context) if isinstance(context, dict) else []
    shape = json.dumps(
        {"topLevel": sorted(value), "context": context_keys, "benchmark": benchmark_keys},
        separators=(",", ":"),
        sort_keys=True,
    )
    declared = value.get("schemaVersion") or value.get("version")
    return {
        "name": "androidx.benchmark.BenchmarkData",
        "declaredVersion": declared,
        "shapeSha256": hashlib.sha256(shape.encode()).hexdigest(),
    }


def benchmark_version(value: dict[str, Any], project_root: Path) -> tuple[str | None, str]:
    context = value.get("context", {})
    payload = context.get("payload", {}) if isinstance(context, dict) else {}
    candidates = (
        value.get("benchmarkVersion"),
        context.get("benchmarkVersion") if isinstance(context, dict) else None,
        payload.get("androidx.benchmark.version") if isinstance(payload, dict) else None,
    )
    for candidate in candidates:
        if isinstance(candidate, str) and candidate:
            return candidate, "input"
    versions = project_root / "gradle" / "libs.versions.toml"
    if versions.is_file():
        match = re.search(r'^benchmark\s*=\s*"([^"]+)"', versions.read_text(encoding="utf-8"), re.MULTILINE)
        if match:
            return match.group(1), "project version catalog"
    return None, "unavailable"


def normalize_benchmark(item: dict[str, Any], source_file: str, context_index: int) -> dict[str, Any]:
    name = str(item.get("name", "<unnamed>"))
    class_name = str(item.get("className", "<unknown>"))
    params = item.get("params") if isinstance(item.get("params"), dict) else {}
    normalized_params = {str(key): str(value) for key, value in sorted(params.items())}
    identity = f"{class_name}.{name}"
    if normalized_params:
        identity += " " + json.dumps(normalized_params, sort_keys=True, separators=(",", ":"))

    metrics: list[dict[str, Any]] = []
    for metric_name, result in sorted((item.get("metrics") or {}).items()):
        if not isinstance(result, dict):
            continue
        statistics = {
            key: number(result.get(key))
            for key in ("minimum", "median", "maximum", "coefficientOfVariation")
            if number(result.get(key)) is not None
        }
        runs = numeric_list(result.get("runs"))
        metrics.append(
            {
                "kind": "single",
                "name": str(metric_name),
                "statistics": statistics,
                "runs": runs,
                "runCount": len(runs),
            }
        )
    for metric_name, result in sorted((item.get("sampledMetrics") or {}).items()):
        if not isinstance(result, dict):
            continue
        statistics = {
            key: number(result.get(key))
            for key in ("P50", "P90", "P95", "P99")
            if number(result.get(key)) is not None
        }
        runs = nested_numeric_lists(result.get("runs"))
        metrics.append(
            {
                "kind": "sampled",
                "name": str(metric_name),
                "statistics": statistics,
                "runs": runs,
                "runCount": len(runs),
                "sampleCount": sum(len(run) for run in runs),
            }
        )
    profiler_outputs = []
    for output in item.get("profilerOutputs") or []:
        if not isinstance(output, dict):
            continue
        profiler_outputs.append(
            {
                key: value
                for key, value in {
                    "type": output.get("type"),
                    "label": output.get("label"),
                    "filename": portable_filename(output.get("filename")),
                }.items()
                if value is not None
            }
        )
    return {
        "id": identity,
        "className": class_name,
        "name": name,
        "params": normalized_params,
        "totalRunTimeNs": number(item.get("totalRunTimeNs")),
        "warmupIterations": number(item.get("warmupIterations")),
        "repeatIterations": number(item.get("repeatIterations")),
        "thermalThrottleSleepSeconds": number(item.get("thermalThrottleSleepSeconds")),
        "metrics": metrics,
        "profilerOutputs": profiler_outputs,
        "sourceFile": source_file,
        "contextIndex": context_index,
    }


def git_metadata(project_root: Path) -> dict[str, Any]:
    def git(*args: str) -> subprocess.CompletedProcess[bytes]:
        return subprocess.run(
            ["git", "-C", str(project_root), *args], check=False, capture_output=True
        )

    head = git("rev-parse", "HEAD")
    if head.returncode != 0:
        return {"head": None, "dirty": None, "diffSha256": None}
    status = git("status", "--porcelain=v1", "--untracked-files=normal")
    dirty = bool(status.stdout)
    diff_hash = None
    if dirty:
        diff = git("diff", "--binary", "HEAD", "--")
        if diff.returncode == 0:
            digest = hashlib.sha256()
            digest.update(status.stdout)
            digest.update(diff.stdout)
            untracked = git("ls-files", "--others", "--exclude-standard", "-z")
            if untracked.returncode == 0:
                for relative_bytes in sorted(filter(None, untracked.stdout.split(b"\0"))):
                    digest.update(relative_bytes)
                    path = project_root / relative_bytes.decode(errors="surrogateescape")
                    if path.is_file():
                        digest.update(path.read_bytes())
            diff_hash = digest.hexdigest()
    return {
        "head": head.stdout.decode().strip(),
        "dirty": dirty,
        "diffSha256": diff_hash,
    }


def portable_metadata(value: Any) -> Any:
    if isinstance(value, dict):
        return {str(key): portable_metadata(item) for key, item in value.items()}
    if isinstance(value, list):
        return [portable_metadata(item) for item in value]
    if isinstance(value, str):
        value = re.sub(r"file:///[^\s,;)\]}]+", "<absolute-path>", value)
        value = re.sub(r"(^|[\s=(\"'])/[^\s,;)\]}]+", r"\1<absolute-path>", value)
        value = re.sub(
            r"(^|[\s=(\"'])[A-Za-z]:[\\/][^\s,;)\]}]+", r"\1<absolute-path>", value
        )
    return value


def apk_metadata(value: dict[str, Any], source: Path) -> dict[str, Any]:
    elements = []
    for element in value.get("elements", []):
        if isinstance(element, dict):
            elements.append(
                {
                    key: item
                    for key, item in {
                        "type": element.get("type"),
                        "versionCode": element.get("versionCode"),
                        "versionName": element.get("versionName"),
                        "outputFile": portable_filename(element.get("outputFile")),
                        "sha256": element.get("sha256"),
                    }.items()
                    if item is not None
                }
            )
    return {
        "sourceFile": source.name,
        "sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
        "applicationId": value.get("applicationId"),
        "variantName": value.get("variantName"),
        "artifactType": value.get("artifactType"),
        "elements": elements,
    }


def archive_name(label: str | None) -> str:
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    if not label:
        return timestamp
    slug = re.sub(r"[^0-9A-Za-z._-]+", "-", label).strip("-._")
    if not slug:
        raise ValueError("label must contain a letter or number")
    return f"{timestamp}-{slug}"


def unique_directory(parent: Path, name: str) -> Path:
    candidate = parent / name
    index = 2
    while candidate.exists():
        candidate = parent / f"{name}-{index}"
        index += 1
    candidate.mkdir(parents=True)
    return candidate


def copy_raw_inputs(
    inputs: list[tuple[Path, dict[str, Any]]], input_root: Path, archive: Path
) -> None:
    raw = archive / "raw"
    for path, _ in inputs:
        relative = Path(portable_relative(path, input_root))
        destination = raw / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, destination)


def copy_trace_artifacts(source: Path, archive: Path, byte_limit: int) -> dict[str, Any]:
    if byte_limit < 0:
        raise ValueError("--max-artifact-bytes cannot be negative")
    if not source.is_dir():
        raise ValueError(f"artifact path is not a directory: {source}")
    candidates = sorted(
        path for path in source.rglob("*") if path.is_file() and path.name.endswith(TRACE_SUFFIXES)
    )
    copied: list[dict[str, Any]] = []
    omitted: list[dict[str, Any]] = []
    used = 0
    for path in candidates:
        size = path.stat().st_size
        relative = path.relative_to(source)
        record = {"path": relative.as_posix(), "bytes": size}
        if used + size > byte_limit:
            omitted.append(record | {"reason": "byte limit"})
            continue
        destination = archive / "artifacts" / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, destination)
        record["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
        copied.append(record)
        used += size
    return {"byteLimit": byte_limit, "copiedBytes": used, "copied": copied, "omitted": omitted}


def archive_rows(summary: dict[str, Any]) -> Iterable[dict[str, Any]]:
    for benchmark in summary["benchmarks"]:
        for metric in benchmark["metrics"]:
            for statistic, value in metric["statistics"].items():
                yield {
                    "benchmarkId": benchmark["id"],
                    "className": benchmark["className"],
                    "name": benchmark["name"],
                    "params": json.dumps(benchmark["params"], sort_keys=True, separators=(",", ":")),
                    "kind": metric["kind"],
                    "metric": metric["name"],
                    "statistic": statistic,
                    "value": value,
                    "runCount": metric["runCount"],
                    "sampleCount": metric.get("sampleCount", ""),
                    "sourceFile": benchmark["sourceFile"],
                }


def write_csv(path: Path, rows: Iterable[dict[str, Any]], fieldnames: list[str]) -> None:
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)


def format_number(value: Any, signed: bool = False) -> str:
    if value is None:
        return "n/a"
    prefix = "+" if signed and value > 0 else ""
    return prefix + f"{value:.6g}"


def archive_markdown(summary: dict[str, Any]) -> str:
    git = summary["git"]
    version = summary["androidxBenchmarkVersion"] or "unknown"
    lines = [
        "# Benchmark archive",
        "",
        f"Archived at: {summary['createdAt']}",
        f"Label: {summary.get('label') or 'none'}",
        f"Git: {git.get('head') or 'unknown'}{' (dirty)' if git.get('dirty') else ''}",
        f"AndroidX Benchmark: {version}",
        "",
        "| Benchmark | Metric | Statistic | Value | Runs |",
        "| --- | --- | --- | ---: | ---: |",
    ]
    for row in archive_rows(summary):
        lines.append(
            f"| `{row['benchmarkId']}` | `{row['metric']}` ({row['kind']}) | "
            f"{row['statistic']} | {format_number(row['value'])} | {row['runCount']} |"
        )
    if summary["warnings"]:
        lines += ["", "## Warnings", ""] + [f"- {warning}" for warning in summary["warnings"]]
    return "\n".join(lines) + "\n"


def create_archive(args: argparse.Namespace, project_root: Path) -> Path:
    inputs = discover_inputs(args.input)
    output_root = args.output_dir.resolve()
    archive = unique_directory(output_root, archive_name(args.label))
    copy_raw_inputs(inputs, args.input, archive)

    contexts: list[dict[str, Any]] = []
    schemas: list[dict[str, Any]] = []
    benchmarks: list[dict[str, Any]] = []
    versions: set[str] = set()
    version_origins: set[str] = set()
    for path, value in inputs:
        context_index = len(contexts)
        contexts.append(normalize_context(value["context"]))
        schemas.append(infer_input_schema(value))
        version, origin = benchmark_version(value, project_root)
        if version:
            versions.add(version)
        version_origins.add(origin)
        source_file = portable_relative(path, args.input)
        for item in value["benchmarks"]:
            if not isinstance(item, dict):
                raise ValueError(f"non-object benchmark entry in {path}")
            benchmarks.append(normalize_benchmark(item, source_file, context_index))
    benchmarks.sort(key=lambda item: (item["id"], item["sourceFile"]))
    benchmark_ids = [item["id"] for item in benchmarks]
    duplicate_ids = sorted({identity for identity in benchmark_ids if benchmark_ids.count(identity) > 1})
    if duplicate_ids:
        raise ValueError(
            "duplicate benchmark results in one archive: " + ", ".join(duplicate_ids)
        )

    warnings: list[str] = []
    if not versions:
        warnings.append("AndroidX Benchmark library version was unavailable.")
    if len(versions) > 1:
        warnings.append("Input files report different AndroidX Benchmark library versions.")
    if len({json.dumps(context, sort_keys=True) for context in contexts}) > 1:
        warnings.append("Input files contain different device or runtime contexts.")
    if any(context.get("emulator") for context in contexts):
        warnings.append(
            "This archive came from an emulator. Use it only for smoke testing, not performance comparison."
        )

    run_metadata = None
    if args.metadata:
        run_metadata = portable_metadata(read_json(args.metadata))
        private = archive / "private"
        private.mkdir(exist_ok=True)
        shutil.copy2(args.metadata, private / "run-metadata.json")
    apk = None
    if args.apk_metadata:
        apk_value = read_json(args.apk_metadata)
        apk = apk_metadata(apk_value, args.apk_metadata)
        private = archive / "private"
        private.mkdir(exist_ok=True)
        shutil.copy2(args.apk_metadata, private / "apk-metadata.json")

    artifacts = None
    if args.artifacts:
        artifacts = copy_trace_artifacts(args.artifacts, archive, args.max_artifact_bytes)
        if artifacts["omitted"]:
            warnings.append(
                f"Skipped {len(artifacts['omitted'])} trace artifact(s) because of the byte limit."
            )

    unique_schemas = {json.dumps(schema, sort_keys=True) for schema in schemas}
    summary = {
        "archiveSchemaVersion": ARCHIVE_SCHEMA_VERSION,
        "createdAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "label": portable_metadata(args.label),
        "git": git_metadata(project_root),
        "androidxBenchmarkVersion": sorted(versions)[0] if len(versions) == 1 else None,
        "androidxBenchmarkVersionSource": sorted(version_origins),
        "inputSchemas": [json.loads(value) for value in sorted(unique_schemas)],
        "contexts": contexts,
        "runMetadata": run_metadata,
        "apkMetadata": apk,
        "artifacts": artifacts,
        "benchmarks": benchmarks,
        "warnings": warnings,
    }
    write_json(archive / "summary.json", summary)
    rows = list(archive_rows(summary))
    write_csv(
        archive / "summary.csv",
        rows,
        [
            "benchmarkId",
            "className",
            "name",
            "params",
            "kind",
            "metric",
            "statistic",
            "value",
            "runCount",
            "sampleCount",
            "sourceFile",
        ],
    )
    (archive / "summary.md").write_text(archive_markdown(summary), encoding="utf-8")
    return archive


def load_archive(path: Path) -> tuple[Path, dict[str, Any]]:
    summary_path = path / "summary.json" if path.is_dir() else path
    value = read_json(summary_path)
    if value.get("archiveSchemaVersion") is None or not isinstance(value.get("benchmarks"), list):
        raise ValueError(f"not a benchmark archive summary: {summary_path}")
    return summary_path.parent, value


def one_or_mixed(values: Iterable[Any]) -> Any:
    encoded = {json.dumps(value, sort_keys=True) for value in values}
    if not encoded:
        return None
    if len(encoded) == 1:
        return json.loads(next(iter(encoded)))
    return "<mixed within archive>"


def path_value(value: dict[str, Any], path: tuple[str, ...]) -> Any:
    current: Any = value
    for key in path:
        if not isinstance(current, dict):
            return None
        current = current.get(key)
    return current


def flatten(value: Any, prefix: str = "") -> dict[str, Any]:
    if not isinstance(value, dict):
        return {prefix: value}
    flattened: dict[str, Any] = {}
    for key, item in value.items():
        path = f"{prefix}.{key}" if prefix else str(key)
        flattened.update(flatten(item, path))
    return flattened


def compatible_metadata(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        return {}
    return flatten(value)


def compatibility(summary: dict[str, Any]) -> dict[str, Any]:
    contexts = summary.get("contexts") or []
    context_fields = {
        "device.brand": ("build", "brand"),
        "device.device": ("build", "device"),
        "device.model": ("build", "model"),
        "device.buildId": ("build", "id"),
        "device.fingerprint": ("build", "fingerprint"),
        "device.api": ("build", "version", "sdk"),
        "device.codename": ("build", "version", "codename"),
        "device.emulator": ("emulator",),
        "build.type": ("build", "type"),
        "runtime.compilationMode": ("compilationMode",),
        "runtime.cpuCoreCount": ("cpuCoreCount",),
        "runtime.cpuLocked": ("cpuLocked",),
        "runtime.cpuMaxFreqHz": ("cpuMaxFreqHz",),
        "runtime.memTotalBytes": ("memTotalBytes",),
        "runtime.sustainedPerformanceModeEnabled": ("sustainedPerformanceModeEnabled",),
        "runtime.artMainlineVersion": ("artMainlineVersion",),
        "runtime.osCodenameAbbreviated": ("osCodenameAbbreviated",),
    }
    result = {
        key: one_or_mixed(path_value(context, path) for context in contexts)
        for key, path in context_fields.items()
    }
    result["schema.archive"] = summary.get("archiveSchemaVersion")
    result["schema.input"] = sorted(
        schema.get("shapeSha256") for schema in summary.get("inputSchemas", []) if schema.get("shapeSha256")
    )
    result["library.androidxBenchmark"] = summary.get("androidxBenchmarkVersion")
    result.update({f"runMetadata.{key}": item for key, item in compatible_metadata(summary.get("runMetadata")).items()})
    apk = summary.get("apkMetadata")
    if isinstance(apk, dict):
        result["build.applicationId"] = apk.get("applicationId")
        result["build.variantName"] = apk.get("variantName")
    for benchmark in summary.get("benchmarks", []):
        identity = benchmark.get("id")
        for key in ("params", "warmupIterations", "repeatIterations", "thermalThrottleSleepSeconds"):
            result[f"benchmark.{identity}.{key}"] = benchmark.get(key)
    return result


def compatibility_mismatches(
    baseline: dict[str, Any], current: dict[str, Any]
) -> list[dict[str, Any]]:
    before = compatibility(baseline)
    after = compatibility(current)
    mismatches = []
    for key in sorted(set(before) | set(after)):
        if before.get(key) != after.get(key):
            mismatches.append({"key": key, "baseline": before.get(key), "current": after.get(key)})
    return mismatches


def metric_values(summary: dict[str, Any]) -> dict[tuple[str, str, str, str], int | float]:
    values = {}
    for benchmark in summary.get("benchmarks", []):
        for metric in benchmark.get("metrics", []):
            for statistic, value in metric.get("statistics", {}).items():
                numeric = number(value)
                if numeric is not None:
                    values[(benchmark["id"], metric["kind"], metric["name"], statistic)] = numeric
    return values


def compare_values(baseline: dict[str, Any], current: dict[str, Any]) -> list[dict[str, Any]]:
    before = metric_values(baseline)
    after = metric_values(current)
    rows = []
    for key in sorted(set(before) | set(after)):
        baseline_value = before.get(key)
        current_value = after.get(key)
        if baseline_value is None:
            status = "missing in baseline"
            delta = percent = None
            percent_reason = None
        elif current_value is None:
            status = "missing in current"
            delta = percent = None
            percent_reason = None
        else:
            status = "matched"
            delta = current_value - baseline_value
            if baseline_value == 0:
                percent = None
                percent_reason = "baseline is zero"
            else:
                percent = delta / abs(baseline_value) * 100
                percent_reason = None
        rows.append(
            {
                "benchmarkId": key[0],
                "kind": key[1],
                "metric": key[2],
                "statistic": key[3],
                "baseline": baseline_value,
                "current": current_value,
                "absoluteDelta": delta,
                "percentDelta": percent,
                "percentDeltaUnavailableReason": percent_reason,
                "status": status,
            }
        )
    return rows


def comparison_markdown(report: dict[str, Any]) -> str:
    lines = [
        "# Benchmark comparison",
        "",
        "These are observed deltas. This report does not test or claim statistical significance.",
        "",
    ]
    if report["warnings"]:
        lines += ["## Warnings", ""] + [f"- {warning}" for warning in report["warnings"]] + [""]
    if report["compatibilityMismatches"]:
        lines += [
            "## Compatibility warnings",
            "",
            "The runs differ in conditions that can affect benchmark results.",
            "",
            "| Condition | Baseline | Current |",
            "| --- | --- | --- |",
        ]
        for mismatch in report["compatibilityMismatches"]:
            lines.append(
                f"| `{mismatch['key']}` | `{json.dumps(mismatch['baseline'], sort_keys=True)}` | "
                f"`{json.dumps(mismatch['current'], sort_keys=True)}` |"
            )
        lines.append("")
    lines += [
        "## Metric deltas",
        "",
        "| Benchmark | Metric | Statistic | Baseline | Current | Delta | Delta % | Status |",
        "| --- | --- | --- | ---: | ---: | ---: | ---: | --- |",
    ]
    for row in report["comparisons"]:
        percent = "n/a" if row["percentDelta"] is None else format_number(row["percentDelta"], True) + "%"
        lines.append(
            f"| `{row['benchmarkId']}` | `{row['metric']}` ({row['kind']}) | {row['statistic']} | "
            f"{format_number(row['baseline'])} | {format_number(row['current'])} | "
            f"{format_number(row['absoluteDelta'], True)} | {percent} | {row['status']} |"
        )
    return "\n".join(lines) + "\n"


def comparison_name(baseline_dir: Path, current_dir: Path) -> str:
    def slug(value: str) -> str:
        return re.sub(r"[^0-9A-Za-z._-]+", "-", value).strip("-._") or "archive"

    return f"{slug(baseline_dir.name)}-vs-{slug(current_dir.name)}"


def create_comparison(args: argparse.Namespace) -> Path:
    baseline_dir, baseline = load_archive(args.baseline)
    current_dir, current = load_archive(args.current)
    output_root = args.output_dir or Path("benchmark-results") / "comparisons"
    destination = unique_directory(output_root.resolve(), comparison_name(baseline_dir, current_dir))
    report = {
        "comparisonSchemaVersion": 1,
        "createdAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "baseline": {
            "archive": baseline_dir.name,
            "label": baseline.get("label"),
            "git": baseline.get("git"),
        },
        "current": {
            "archive": current_dir.name,
            "label": current.get("label"),
            "git": current.get("git"),
        },
        "compatibilityMismatches": compatibility_mismatches(baseline, current),
        "warnings": [f"Baseline archive: {warning}" for warning in baseline.get("warnings", [])]
        + [f"Current archive: {warning}" for warning in current.get("warnings", [])],
        "comparisons": compare_values(baseline, current),
        "interpretation": "Observed deltas only; no statistical significance is claimed.",
    }
    write_json(destination / "comparison.json", report)
    write_csv(
        destination / "comparison.csv",
        report["comparisons"],
        [
            "benchmarkId",
            "kind",
            "metric",
            "statistic",
            "baseline",
            "current",
            "absoluteDelta",
            "percentDelta",
            "percentDeltaUnavailableReason",
            "status",
        ],
    )
    (destination / "comparison.md").write_text(comparison_markdown(report), encoding="utf-8")
    return destination


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    project_root = Path(__file__).resolve().parents[1]
    try:
        destination = create_archive(args, project_root) if args.command == "archive" else create_comparison(args)
    except (OSError, ValueError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2
    print(destination)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
