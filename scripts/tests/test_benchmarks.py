"""Tests for AndroidX benchmark archive and comparison reports."""

import csv
import hashlib
import tracemalloc
import importlib.util
import json
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path


spec = importlib.util.spec_from_file_location("benchmarks", Path(__file__).parents[1] / "benchmarks.py")
benchmarks = importlib.util.module_from_spec(spec)
spec.loader.exec_module(benchmarks)


def androidx_result(median=12.0, repeat_iterations=5, model="Pixel 9", sdk=35, emulator=False):
    device = "tokay"
    fingerprint = "google/tokay/tokay:15/AP4A/release-keys"
    if emulator:
        device = "emu64a"
        fingerprint = "google/sdk_gphone64_arm64/emu64a:15/eng/test-keys"
    return {
        "context": {
            "build": {
                "brand": "google",
                "device": device,
                "fingerprint": fingerprint,
                "id": "AP4A",
                "model": model,
                "type": "user",
                "version": {"codename": "REL", "sdk": sdk},
            },
            "cpuCoreCount": 8,
            "cpuLocked": False,
            "cpuMaxFreqHz": 3200000000,
            "memTotalBytes": 12000000000,
            "sustainedPerformanceModeEnabled": False,
            "artMainlineVersion": 351000000,
            "osCodenameAbbreviated": "A",
            "compilationMode": "speed-profile",
            "payload": {},
        },
        "benchmarks": [
            {
                "name": "startupFullCompilation",
                "params": {"startupMode": "COLD"},
                "className": "com.example.benchmark.ColdStartupBenchmark",
                "totalRunTimeNs": 999000000,
                "metrics": {
                    "timeToInitialDisplayMs": {
                        "minimum": 10.0,
                        "maximum": 14.0,
                        "median": median,
                        "coefficientOfVariation": 0.1,
                        "runs": [10.0, median, 14.0],
                    },
                    "zeroMetric": {
                        "minimum": 0.0,
                        "maximum": 0.0,
                        "median": 0.0,
                        "coefficientOfVariation": 0.0,
                        "runs": [0.0],
                    },
                },
                "sampledMetrics": {
                    "frameDurationCpuMs": {
                        "P50": 4.0,
                        "P90": 7.0,
                        "P95": 8.0,
                        "P99": 9.0,
                        "runs": [[3.0, 4.0], [4.0, 9.0]],
                    }
                },
                "warmupIterations": 0,
                "repeatIterations": repeat_iterations,
                "thermalThrottleSleepSeconds": 0,
                "profilerOutputs": [
                    {
                        "type": "PerfettoTrace",
                        "label": "Trace 0",
                        "filename": "/data/user/0/app/ColdStartup_0.perfetto-trace",
                    }
                ],
            }
        ],
    }


class ArchiveTest(unittest.TestCase):
    def test_archive_parses_androidx_1_5_shape_and_copies_bounded_traces(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            nested = root / "input" / "device"
            nested.mkdir(parents=True)
            source = nested / "app-benchmarkData.json"
            source.write_text(json.dumps(androidx_result()), encoding="utf-8")
            (nested / "not-a-result.json").write_text('{"value": 1}', encoding="utf-8")
            artifacts = root / "artifacts"
            artifacts.mkdir()
            (artifacts / "small.perfetto-trace").write_bytes(b"1234")
            (artifacts / "large.perfetto-trace").write_bytes(b"12345678")
            metadata = root / "run.json"
            metadata.write_text(
                json.dumps(
                    {
                        "benchmarkIterations": 5,
                        "display": {"size": "1080x2424", "density": 420, "fontScale": 1.0},
                        "suiteCommand": "build-brief ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest",
                    }
                ),
                encoding="utf-8",
            )
            args = Namespace(
                input=root / "input",
                output_dir=root / "archives",
                label="physical-pixel",
                metadata=metadata,
                apk_metadata=None,
                artifacts=artifacts,
                max_artifact_bytes=6,
            )
            archive = benchmarks.create_archive(args, Path(__file__).parents[2])
            summary = json.loads((archive / "summary.json").read_text(encoding="utf-8"))

            self.assertEqual("1.5.0", summary["androidxBenchmarkVersion"])
            self.assertEqual("Pixel 9", summary["contexts"][0]["build"]["model"])
            self.assertFalse(summary["contexts"][0]["emulator"])
            self.assertEqual("device/app-benchmarkData.json", summary["benchmarks"][0]["sourceFile"])
            metrics = {item["name"]: item for item in summary["benchmarks"][0]["metrics"]}
            self.assertEqual([10.0, 12.0, 14.0], metrics["timeToInitialDisplayMs"]["runs"])
            self.assertEqual(7.0, metrics["frameDurationCpuMs"]["statistics"]["P90"])
            self.assertEqual(2, metrics["frameDurationCpuMs"]["runCount"])
            self.assertEqual(4, metrics["frameDurationCpuMs"]["sampleCount"])
            self.assertEqual("ColdStartup_0.perfetto-trace", summary["benchmarks"][0]["profilerOutputs"][0]["filename"])
            self.assertTrue((archive / "raw" / "device" / source.name).is_file())
            self.assertEqual(1, len(summary["artifacts"]["copied"]))
            self.assertEqual(1, len(summary["artifacts"]["omitted"]))
            self.assertTrue((archive / "summary.csv").is_file())
            self.assertIn("timeToInitialDisplayMs", (archive / "summary.md").read_text(encoding="utf-8"))

    def test_archive_rejects_an_unrelated_json_file(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "result.json"
            path.write_text('{"benchmarks": []}', encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "not AndroidX BenchmarkData"):
                benchmarks.discover_inputs(path)

    def test_portable_metadata_removes_embedded_unix_uri_and_windows_paths(self):
        value = benchmarks.portable_metadata(
            {
                "args": "--output=/Users/alice/run file:///private/tmp/x C:\\Users\\alice\\run",
                "url": "https://example.com/results",
            }
        )
        self.assertEqual(
            "--output=<absolute-path> <absolute-path> <absolute-path>", value["args"]
        )
        self.assertEqual("https://example.com/results", value["url"])


class ComparisonTest(unittest.TestCase):
    def summary(self, value, **metadata):
        source = androidx_result(median=value, **metadata)
        return {
            "archiveSchemaVersion": 1,
            "androidxBenchmarkVersion": "1.5.0",
            "inputSchemas": [benchmarks.infer_input_schema(source)],
            "contexts": [benchmarks.normalize_context(source["context"])],
            "runMetadata": {
                "benchmarkIterations": metadata.get("repeat_iterations", 5),
                "display": {"size": "1080x2424", "density": 420, "fontScale": 1.0},
                "animationScales": {
                    "window_animation_scale": "0",
                    "transition_animation_scale": "0",
                    "animator_duration_scale": "0",
                },
                "benchmarkAllowEmulator": metadata.get("emulator", False),
            },
            "apkMetadata": {"applicationId": "dev.staticvar.vlr", "variantName": "benchmarkRelease"},
            "benchmarks": [benchmarks.normalize_benchmark(source["benchmarks"][0], "result.json", 0)],
        }

    def test_comparison_has_signed_absolute_and_percent_deltas(self):
        rows = benchmarks.compare_values(self.summary(10.0), self.summary(12.5))
        median = next(
            row
            for row in rows
            if row["metric"] == "timeToInitialDisplayMs" and row["statistic"] == "median"
        )
        zero = next(row for row in rows if row["metric"] == "zeroMetric" and row["statistic"] == "median")
        self.assertEqual(2.5, median["absoluteDelta"])
        self.assertEqual(25.0, median["percentDelta"])
        self.assertIsNone(zero["percentDelta"])
        self.assertEqual("baseline is zero", zero["percentDeltaUnavailableReason"])

    def test_comparison_keeps_missing_metrics_without_dividing(self):
        baseline = self.summary(10.0)
        current = self.summary(12.0)
        current["benchmarks"][0]["metrics"] = [
            metric for metric in current["benchmarks"][0]["metrics"] if metric["name"] != "zeroMetric"
        ]
        row = next(row for row in benchmarks.compare_values(baseline, current) if row["metric"] == "zeroMetric")
        self.assertEqual("missing in current", row["status"])
        self.assertIsNone(row["absoluteDelta"])

    def test_comparison_flags_device_emulator_iterations_and_run_conditions(self):
        baseline = self.summary(10.0)
        current = self.summary(12.0, repeat_iterations=10, model="sdk_gphone64_arm64", sdk=36, emulator=True)
        current["runMetadata"]["display"]["fontScale"] = 1.15
        current["contexts"][0]["build"]["id"] = "BP1A"
        current["contexts"][0]["cpuMaxFreqHz"] = 3000000000
        keys = {item["key"] for item in benchmarks.compatibility_mismatches(baseline, current)}
        self.assertIn("device.model", keys)
        self.assertIn("device.api", keys)
        self.assertIn("device.emulator", keys)
        self.assertIn("device.buildId", keys)
        self.assertIn("runtime.cpuMaxFreqHz", keys)
        self.assertIn("benchmarkAllowEmulator", " ".join(keys))
        self.assertIn("benchmark.com.example.benchmark.ColdStartupBenchmark.startupFullCompilation {\"startupMode\":\"COLD\"}.repeatIterations", keys)
        self.assertTrue(any("fontScale" in key for key in keys))

    def test_emulator_warning_is_carried_into_comparison_markdown(self):
        baseline = self.summary(10.0)
        current = self.summary(12.0, model="sdk_gphone64_arm64", emulator=True)
        baseline["warnings"] = []
        current["warnings"] = [
            "This archive came from an emulator. Use it only for smoke testing, not performance comparison."
        ]
        report = {
            "warnings": [f"Current archive: {current['warnings'][0]}"],
            "compatibilityMismatches": benchmarks.compatibility_mismatches(baseline, current),
            "comparisons": benchmarks.compare_values(baseline, current),
        }
        markdown = benchmarks.comparison_markdown(report)
        self.assertIn("Current archive: This archive came from an emulator", markdown)
        self.assertIn("does not test or claim statistical significance", markdown)




class ReportSafetyTest(unittest.TestCase):
    def test_portable_filenames_across_platforms(self):
        for source in (
            "/Users/alice/run.trace",
            r"C:\Users\alice\run.trace",
            r"\\server\share\run.trace",
            r"..\private\run.trace",
            "../private/run.trace",
        ):
            with self.subTest(source=source):
                self.assertEqual("run.trace", benchmarks.portable_filename(source))
        self.assertEqual("device/run.trace", benchmarks.portable_filename("device/run.trace"))

    def test_archive_and_comparison_csv_escape_formulas_preserving_numbers(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            for prefix in ("=", "+", "-", "@", "\t", "\r", "\n"):
                source = androidx_result()
                item = source["benchmarks"][0]
                item["className"] = prefix + "Class"
                item["metrics"] = {prefix + "metric": {"median": -2.0, "runs": [-2.0]}}
                item["sampledMetrics"] = {}
                summary = {"benchmarks": [benchmarks.normalize_benchmark(item, "result.json", 0)]}
                for name, rows in (
                    ("summary", list(benchmarks.archive_rows(summary))),
                    ("comparison", benchmarks.compare_values(summary, summary)),
                ):
                    with self.subTest(prefix=prefix, report=name):
                        path = root / (name + ".csv")
                        benchmarks.write_csv(path, rows, list(rows[0]))
                        with path.open(newline="") as stream:
                            row = next(csv.DictReader(stream))
                        self.assertTrue(row["benchmarkId"].startswith("'" + prefix))
                        self.assertEqual("'" + prefix + "metric", row["metric"])
                        self.assertEqual("-2.0", row["value" if name == "summary" else "baseline"])

    def test_trace_hash_is_correct_with_bounded_memory(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "input"
            source.mkdir()
            chunk = b"trace" * 200_000
            digest = hashlib.sha256()
            with (source / "large.trace").open("wb") as stream:
                for _ in range(16):
                    stream.write(chunk)
                    digest.update(chunk)
            tracemalloc.start()
            try:
                result = benchmarks.copy_trace_artifacts(source, root / "archive", 20_000_000)
                _, peak = tracemalloc.get_traced_memory()
            finally:
                tracemalloc.stop()
            self.assertEqual(digest.hexdigest(), result["copied"][0]["sha256"])
            self.assertLess(peak, 5_000_000)


if __name__ == "__main__":
    unittest.main()
