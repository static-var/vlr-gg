#!/usr/bin/env python3
"""Run the Android macrobenchmarks and archive their reports and traces."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
import uuid
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]


def positive(value):
    number = int(value)
    if number <= 0:
        raise argparse.ArgumentTypeError("iterations must be positive")
    return number


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", required=True, help="adb serial; only this device is used")
    parser.add_argument("--allow-emulator", action="store_true", help="collect emulator smoke results")
    parser.add_argument("--iterations", type=positive, default=5)
    parser.add_argument("--label", default="benchmark")
    parser.add_argument("--class", dest="test_class", help="optional fully qualified benchmark class")
    parser.add_argument("--output-dir", type=Path, default=ROOT / "benchmark-results")
    args = parser.parse_args()
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    adb = shutil.which("adb")
    if not adb and sdk:
        adb = str(Path(sdk) / "platform-tools" / "adb")
    if not adb:
        candidate = Path.home() / "Library/Android/sdk/platform-tools/adb"
        if candidate.is_file():
            adb = str(candidate)
    if not adb:
        parser.error("adb was not found; set ANDROID_HOME or add platform-tools to PATH")
    if not shutil.which("build-brief"):
        parser.error("build-brief must be available on PATH")

    def shell(*command):
        return subprocess.check_output([adb, "-s", args.device, "shell", *command], text=True).strip()

    if subprocess.check_output([adb, "-s", args.device, "get-state"], text=True).strip() != "device":
        parser.error("selected device is not ready")
    emulator = shell("getprop", "ro.kernel.qemu") == "1" or args.device.startswith("emulator-")
    if emulator and not args.allow_emulator:
        parser.error("emulator results are smoke checks; pass --allow-emulator explicitly")
    run_id = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "-" + uuid.uuid4().hex[:8]
    staging = args.output_dir.resolve() / "pending" / run_id
    staging.mkdir(parents=True)
    metadata = {
        "benchmarkIterations": args.iterations,
        "deviceSerial": args.device,
        "benchmarkAllowEmulator": args.allow_emulator,
        "emulator": emulator,
        "display": {"size": shell("wm", "size"), "density": shell("wm", "density"),
                    "fontScale": shell("settings", "get", "system", "font_scale")},
        "animationScales": {name: shell("settings", "get", "global", name) for name in
                            ["window_animation_scale", "transition_animation_scale", "animator_duration_scale"]},
        "suiteCommand": ":benchmark:connectedBenchmarkReleaseAndroidTest",
        "testClass": args.test_class or "all",
        "buildType": "benchmarkRelease",
        "dataPolicy": "Existing app data and live backend; English locale; compact phone layout for detail openings; first/second/third are process-local openings, not cleared disk caches",
    }
    metadata_path = staging / "run-metadata.json"
    metadata_path.write_text(json.dumps(metadata, indent=2) + "\n")
    command = ["build-brief", "./gradlew", ":benchmark:connectedBenchmarkReleaseAndroidTest",
               "-Pandroid.testInstrumentationRunnerArguments.benchmarkIterations=" + str(args.iterations)]
    if args.allow_emulator:
        command.append("-PbenchmarkAllowEmulator=true")
    if args.test_class:
        command.append("-Pandroid.testInstrumentationRunnerArguments.class=" + args.test_class)
    env = dict(os.environ, ANDROID_SERIAL=args.device)
    print("Run files: " + str(staging), flush=True)
    started = time.time()
    with (staging / "build.log").open("w") as log:
        result = subprocess.run(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        print("Benchmark failed; see " + str(staging / "build.log"), file=sys.stderr)
        return result.returncode
    raw = staging / "raw"
    host_output = ROOT / "benchmark/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected"
    raw.mkdir()
    for source in host_output.rglob("*"):
        if source.is_file() and source.stat().st_mtime >= started:
            destination = raw / source.relative_to(host_output)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    if not list(raw.rglob("*.json")):
        raise ValueError("No fresh benchmark JSON was collected; inspect " + str(staging / "build.log"))
    apk_dir = ROOT / "androidApp/build/outputs/apk/benchmarkRelease"
    apk_metadata = json.loads((apk_dir / "output-metadata.json").read_text())
    for element in apk_metadata.get("elements", []):
        apk = apk_dir / element["outputFile"]
        element["sha256"] = hashlib.sha256(apk.read_bytes()).hexdigest()
    apk_metadata_path = staging / "apk-metadata.json"
    apk_metadata_path.write_text(json.dumps(apk_metadata, indent=2) + "\n")
    subprocess.run([sys.executable, str(ROOT / "scripts/benchmarks.py"), "archive", str(raw),
                    "--output-dir", str(args.output_dir.resolve()), "--label", args.label,
                    "--metadata", str(metadata_path), "--apk-metadata", str(apk_metadata_path),
                    "--artifacts", str(raw)], cwd=ROOT, check=True)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
