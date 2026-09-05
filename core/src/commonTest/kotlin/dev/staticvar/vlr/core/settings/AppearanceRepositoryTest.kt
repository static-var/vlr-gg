/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppearanceRepositoryTest {
  @Test
  fun freshInstallFollowsSystemUntilModeIsChosen() {
    val repository = AppearanceRepository(MapSettings())
    assertFalse(repository.settings.value.isDark(systemIsDark = false))
    assertTrue(repository.settings.value.isDark(systemIsDark = true))
    repository.setMode(AppearanceMode.Light)
    assertFalse(repository.settings.value.isDark(systemIsDark = true))
    repository.setMode(AppearanceMode.Dark)
    assertTrue(repository.settings.value.isDark(systemIsDark = false))
  }

  @Test
  fun choicesAreRestoredBeforeFirstStateAndRemainIndependent() {
    val storage = MapSettings()
    val repository = AppearanceRepository(storage)
    repository.setFamily(ThemeFamily.Catppuccin)
    repository.setMode(AppearanceMode.Dark)
    val restored = AppearanceRepository(storage)
    assertEquals(AppearanceSettings(AppearanceMode.Dark, ThemeFamily.Catppuccin), restored.settings.value)
    restored.setFamily(ThemeFamily.Brutalist)
    assertEquals(AppearanceMode.Dark, restored.settings.value.mode)
    assertEquals(restored.settings.value, AppearanceRepository(storage).settings.value)
  }

  @Test
  fun unknownStoredValuesUseDefaults() {
    val storage = MapSettings()
    storage.putString("appearance.mode", "unknown")
    storage.putString("appearance.family", "unknown")
    assertEquals(AppearanceSettings(), AppearanceRepository(storage).settings.value)
  }
}
