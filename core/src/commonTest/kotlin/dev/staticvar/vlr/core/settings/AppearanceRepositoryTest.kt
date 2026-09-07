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
  fun consoleRestoresSystemAndExplicitModesWithoutChangingCatppuccinFlavour() {
    val storage = MapSettings()
    val repository = AppearanceRepository(storage)
    repository.setCatppuccinFlavour(CatppuccinFlavour.Latte)
    repository.setFamily(ThemeFamily.Console)

    val systemAppearance = AppearanceRepository(storage).settings.value
    assertEquals(ThemeFamily.Console, systemAppearance.family)
    assertEquals(null, systemAppearance.mode)
    assertFalse(systemAppearance.isDark(systemIsDark = false))
    assertTrue(systemAppearance.isDark(systemIsDark = true))

    AppearanceMode.entries.forEach { mode ->
      repository.setMode(mode)
      val restored = AppearanceRepository(storage)
      assertEquals(ThemeFamily.Console, restored.settings.value.family)
      assertEquals(mode, restored.settings.value.mode)
      assertEquals(mode == AppearanceMode.Dark, restored.settings.value.isDark(systemIsDark = false))
      assertEquals(mode == AppearanceMode.Dark, restored.settings.value.isDark(systemIsDark = true))
      restored.setFamily(ThemeFamily.Catppuccin)
      assertEquals(CatppuccinFlavour.Latte, restored.settings.value.catppuccinFlavour)
      assertFalse(restored.settings.value.isDark(systemIsDark = true))
      restored.setFamily(ThemeFamily.Console)
      assertEquals(mode, AppearanceRepository(storage).settings.value.mode)
    }
  }

  @Test
  fun unknownStoredValuesUseDefaults() {
    val storage = MapSettings()
    storage.putString("appearance.mode", "unknown")
    storage.putString("appearance.family", "unknown")
    storage.putString("appearance.catppuccin_flavour", "unknown")
    assertEquals(AppearanceSettings(), AppearanceRepository(storage).settings.value)
  }

  @Test
  fun eachFlavourPersistsAndDeterminesBrightnessIndependentlyOfBrutalistMode() {
    val storage = MapSettings()
    val repository = AppearanceRepository(storage)
    repository.setMode(AppearanceMode.Light)
    repository.setFamily(ThemeFamily.Catppuccin)
    CatppuccinFlavour.entries.forEach { flavour ->
      repository.setCatppuccinFlavour(flavour)
      val restored = AppearanceRepository(storage).settings.value
      assertEquals(flavour, restored.catppuccinFlavour)
      assertEquals(AppearanceMode.Light, restored.mode)
      assertEquals(flavour != CatppuccinFlavour.Latte, restored.isDark(systemIsDark = false))
      assertEquals(flavour != CatppuccinFlavour.Latte, restored.isDark(systemIsDark = true))
    }
  }

  @Test
  fun switchingFamiliesPreservesBothChoicesAcrossRestart() {
    val storage = MapSettings()
    val repository = AppearanceRepository(storage)
    repository.setMode(AppearanceMode.Dark)
    repository.setFamily(ThemeFamily.Catppuccin)
    repository.setCatppuccinFlavour(CatppuccinFlavour.Latte)
    assertFalse(repository.settings.value.isDark(systemIsDark = true))
    repository.setFamily(ThemeFamily.Brutalist)
    assertTrue(repository.settings.value.isDark(systemIsDark = false))
    val restored = AppearanceRepository(storage)
    restored.setFamily(ThemeFamily.Catppuccin)
    assertEquals(CatppuccinFlavour.Latte, restored.settings.value.catppuccinFlavour)
    assertFalse(restored.settings.value.isDark(systemIsDark = true))
    assertEquals(AppearanceMode.Dark, restored.settings.value.mode)
  }

  @Test
  fun untouchedFlavourDefaultDoesNotChangeWhenBrutalistModeChanges() {
    val storage = MapSettings()
    val repository = AppearanceRepository(storage)
    repository.setMode(AppearanceMode.Light)
    assertEquals(repository.settings.value, AppearanceRepository(storage).settings.value)
    assertEquals(CatppuccinFlavour.Frappe, repository.settings.value.catppuccinFlavour)
  }
}
