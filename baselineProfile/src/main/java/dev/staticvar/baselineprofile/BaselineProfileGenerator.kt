/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
  @get:Rule
  val baselineProfileRule = BaselineProfileRule()

  @Before
  fun prepareApp() = configureProfileSettings()

  @Test
  fun matches() = collect { browseMatches() }

  @Test
  fun events() = collect { browseEvents() }

  @Test
  fun teamsAndPlayers() = collect { browseTeamAndPlayer() }

  @Test
  fun newsAndSettings() = collect { browseNewsAndSettings() }

  private fun collect(journey: UiDevice.() -> Unit) =
    baselineProfileRule.collect(packageName = TARGET_PACKAGE, maxIterations = 5) {
      startup()
      device.journey()
    }
}
