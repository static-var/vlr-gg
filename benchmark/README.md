# Android macrobenchmarks

Run benchmarks on a dedicated physical device. Use the same device, Android build,
display settings, animation scales, app data policy, and iteration count for every
run that you intend to compare. Close other apps and let a hot device cool before
collecting results.

The runner selects one adb device, records its run conditions, runs the optimized
`benchmarkRelease` suite, pulls AndroidX JSON and Perfetto traces, records APK
metadata, and creates an archive under `benchmark-results/`:

```sh
python3 scripts/run-benchmarks.py \
  --device SERIAL \
  --iterations 5 \
  --label before-change
```

Use `adb devices` to find `SERIAL`. Add `--class` with a fully qualified benchmark
class to run a smaller subset. The full Gradle task used by the runner is:

```sh
build-brief ./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest
```

The runner rejects emulators by default. `--allow-emulator` opts into an emulator
run for smoke testing the journey. Emulator timings are not suitable for performance
comparison, even against another run from the same emulator.

## Archive an existing result

`scripts/benchmarks.py` can archive AndroidX output without running Gradle or adb.
Its input can be one `*-benchmarkData.json` file or a directory. Directory input is
searched recursively.

```sh
python3 scripts/benchmarks.py archive path/to/androidx-output \
  --label after-change \
  --metadata path/to/run-metadata.json \
  --apk-metadata path/to/output-metadata.json \
  --artifacts path/to/androidx-output
```

`--metadata`, `--apk-metadata`, and `--artifacts` are optional. Trace collection only
happens when `--artifacts` names a directory. The default trace limit is 512 MiB per
archive; change it with `--max-artifact-bytes`. Files beyond the limit are listed as
omitted in `summary.json`.

Each archive contains the original AndroidX JSON under `raw/` and normalized
`summary.json`, `summary.csv`, and `summary.md` reports. The JSON report retains the
single metric runs, sampled metric runs and percentiles, AndroidX context, detected
Benchmark library version, input schema shape, Git HEAD and dirty state. A dirty
archive includes a SHA-256 hash of the Git diff and untracked file state. Optional source metadata is
kept in `private/`; portable reports omit absolute path tokens.

## Compare archives

Pass either archive directories or their `summary.json` files:

```sh
python3 scripts/benchmarks.py compare \
  benchmark-results/20260915T100000Z-before-change \
  benchmark-results/20260915T110000Z-after-change
```

The command writes `comparison.json`, `comparison.csv`, and `comparison.md` under
`benchmark-results/comparisons/` unless `--output-dir` is set. Absolute delta is
`current - baseline`. Percentage delta uses the absolute baseline as its denominator.
A missing metric or zero baseline produces `n/a` instead of an invalid percentage.

Compatibility warnings call out changes in device model, API level, emulator state,
build type, AndroidX/input schema, compilation mode, benchmark parameters, iteration
counts, thermal throttle sleep, and recorded run metadata such as display and animation
settings. Fix those differences and rerun before drawing a performance conclusion.
The comparison reports observed deltas only. They do not calculate or claim statistical
significance.
