/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.benchmark

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class ColdStartupBenchmark : StartupBenchmark(StartupMode.COLD)

@RunWith(AndroidJUnit4::class)
public class WarmStartupBenchmark : StartupBenchmark(StartupMode.WARM)

@RunWith(AndroidJUnit4::class)
public class HotStartupBenchmark : StartupBenchmark(StartupMode.HOT)

public abstract class StartupBenchmark(private val startupMode: StartupMode) {
  @get:Rule
  public val benchmarkRule: MacrobenchmarkRule = MacrobenchmarkRule()

  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  public fun startupCompilationNone(): Unit = measureStartup(CompilationMode.None())

  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  public fun startupCompilationPartialDisabledWarmup(): Unit = measureStartup(
    CompilationMode.Partial(
      baselineProfileMode = BaselineProfileMode.Disable,
      warmupIterations = 1,
    ),
  )

  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  public fun startupCompilationPartialRequire(): Unit = measureStartup(
    CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
  )

  @Test
  public fun startupCompilationFull(): Unit = measureStartup(CompilationMode.Full())

  private fun measureStartup(compilationMode: CompilationMode): Unit = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(StartupTimingMetric()),
    compilationMode = compilationMode,
    iterations = benchmarkIterations(),
    startupMode = startupMode,
    setupBlock = { pressHome() },
  ) {
    startAndAssertHome()
  }
}
