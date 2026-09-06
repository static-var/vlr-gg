/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDetailsPreferencesRepositoryTest {
  @Test
  fun freshInstallShowsEverySection() {
    assertEquals(MatchDetailsPreferences(), MatchDetailsPreferencesRepository(MapSettings()).preferences.value)
  }

  @Test
  fun everyCombinationSurvivesRestartWithoutChangingAppearance() {
    val storage = MapSettings()
    val appearance = AppearanceRepository(storage)
    appearance.setFamily(ThemeFamily.Catppuccin)
    appearance.setCatppuccinFlavour(CatppuccinFlavour.Latte)
    val repository = MatchDetailsPreferencesRepository(storage)

    for (showBreakdown in listOf(true, false)) {
      for (showMedia in listOf(true, false)) {
        for (showHeadToHead in listOf(true, false)) {
          val preferences = MatchDetailsPreferences(showBreakdown, showMedia, showHeadToHead)
          repository.setPreferences(preferences)
          assertEquals(preferences, repository.preferences.value)
          assertEquals(preferences, MatchDetailsPreferencesRepository(storage).preferences.value)
          assertEquals(appearance.settings.value, AppearanceRepository(storage).settings.value)
        }
      }
    }
  }

  @Test
  fun unsetSectionKeysRemainVisibleWhenOtherChoicesAlreadyExist() {
    val storage = MapSettings()
    storage.putBoolean("match_details.show_media", false)
    assertEquals(
      MatchDetailsPreferences(showMedia = false),
      MatchDetailsPreferencesRepository(storage).preferences.value,
    )
  }
}
