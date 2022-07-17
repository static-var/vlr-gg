package com.example.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces for
 * investigating your app's performance from a cold state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ColdStartupBenchmark : AbstractStartupBenchmark(StartupMode.COLD)

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces for
 * investigating your app's performance from a warm state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class WarmStartupBenchmark : AbstractStartupBenchmark(StartupMode.WARM)

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces for
 * investigating your app's performance from a hot state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class HotStartupBenchmark : AbstractStartupBenchmark(StartupMode.HOT)

/**
 * Base class for benchmarks with different startup modes. Enables app startups from various states
 * of baseline profile or [CompilationMode]s.
 */
abstract class AbstractStartupBenchmark(private val startupMode: StartupMode) {
  @get:Rule val benchmarkRule = MacrobenchmarkRule()

  @Test fun startupNoCompilation() = startup(CompilationMode.None())

  @Test
  fun startupBaselineProfileDisabled() =
    startup(
      CompilationMode.Partial(
        baselineProfileMode = BaselineProfileMode.Disable,
        warmupIterations = 1
      )
    )

  @Test
  fun startupBaselineProfile() =
    startup(CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require))

  @Test fun startupFullCompilation() = startup(CompilationMode.Full())

  private fun startup(compilationMode: CompilationMode) =
    benchmarkRule.measureRepeated(
      packageName = "dev.staticvar.vlr",
      metrics = listOf(StartupTimingMetric()),
      compilationMode = compilationMode,
      iterations = 5,
      startupMode = startupMode,
      setupBlock = { pressHome() }
    ) { userflow() }
}

// ColdStartupBenchmark_startupNoCompilation
// timeToInitialDisplayMs   min 177.2,   median 204.3,   max 263.0
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// ColdStartupBenchmark_startupFullCompilation
// timeToInitialDisplayMs   min 222.1,   median 234.4,   max 248.0
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// ColdStartupBenchmark_startupBaselineProfile
// timeToInitialDisplayMs   min 170.6,   median 198.8,   max 214.0
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// ColdStartupBenchmark_startupBaselineProfileDisabled
// timeToInitialDisplayMs   min 184.9,   median 200.0,   max 217.7
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9

// HotStartupBenchmark_startupNoCompilation
// timeToInitialDisplayMs   min 44.8,   median 56.5,   max 62.1
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// HotStartupBenchmark_startupFullCompilation
// timeToInitialDisplayMs   min 42.3,   median 51.5,   max 61.2
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// HotStartupBenchmark_startupBaselineProfile
// timeToInitialDisplayMs   min 43.1,   median 54.2,   max 64.8
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9
// HotStartupBenchmark_startupBaselineProfileDisabled
// timeToInitialDisplayMs   min 43.0,   median 50.5,   max 59.8
// Traces: Iteration 0 1 2 3 4 5 6 7 8 9

// timeToInitialDisplayMs   min 52.4,   median 63.5,   max 91.6
// Traces: Iteration 0 1 2 3 4
// HotStartupBenchmark_startupFullCompilation
// timeToInitialDisplayMs   min 49.5,   median 56.8,   max 73.4
// Traces: Iteration 0 1 2 3 4
// HotStartupBenchmark_startupBaselineProfile
// timeToInitialDisplayMs   min 63.5,   median 71.3,   max 90.0
// Traces: Iteration 0 1 2 3 4
// HotStartupBenchmark_startupBaselineProfileDisabled
// timeToInitialDisplayMs   min 59.1,   median 93.5,   max 99.8
// Traces: Iteration 0 1 2 3 4

// timeToInitialDisplayMs   min 211.8,   median 227.3,   max 320.6
// Traces: Iteration 0 1 2 3 4
// ColdStartupBenchmark_startupFullCompilation
// timeToInitialDisplayMs   min 258.9,   median 269.9,   max 280.2
// Traces: Iteration 0 1 2 3 4
// ColdStartupBenchmark_startupBaselineProfile
// timeToInitialDisplayMs   min 214.3,   median 236.4,   max 267.0
// Traces: Iteration 0 1 2 3 4
// ColdStartupBenchmark_startupBaselineProfileDisabled
// timeToInitialDisplayMs   min 211.2,   median 238.8,   max 349.4
// Traces: Iteration 0 1 2 3 4
