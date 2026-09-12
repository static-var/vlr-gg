/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StartupProfileGenerator {
  @get:Rule
  val baselineProfileRule = BaselineProfileRule()

  @Before
  fun prepareApp() = configureProfileSettings()

  @Test
  fun coldStartup() = baselineProfileRule.collect(
    packageName = TARGET_PACKAGE,
    maxIterations = 5,
    includeInStartupProfile = true,
  ) { startup() }
}
