/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.benchmark

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class FrameTimingBenchmark {
  @get:Rule
  public val benchmarkRule: MacrobenchmarkRule = MacrobenchmarkRule()

  @RequiresApi(Build.VERSION_CODES.N)
  @Test
  public fun settingsAboutNavigation(): Unit = benchmarkRule.measureRepeated(
    packageName = TARGET_PACKAGE,
    metrics = listOf(FrameTimingMetric()),
    compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
    iterations = benchmarkIterations(),
    setupBlock = {
      pressHome()
      startAndAssertHome()
    },
  ) {
    device.navigateSettingsAndAbout()
  }
}
