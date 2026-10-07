/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RankingsPreferencesRepositoryTest {
  @Test
  fun regionalIsDisabledByDefault() {
    assertFalse(RankingsPreferencesRepository(MapSettings()).regionalEnabled.value)
  }

  @Test
  fun regionalPreferenceUpdatesObserversAndSurvivesRestartInBothDirections() {
    val storage = MapSettings()
    val repository = RankingsPreferencesRepository(storage)
    val observed = repository.regionalEnabled

    repository.setRegionalEnabled(true)
    assertTrue(observed.value)
    val restarted = RankingsPreferencesRepository(storage)
    assertTrue(restarted.regionalEnabled.value)

    restarted.setRegionalEnabled(false)
    assertFalse(restarted.regionalEnabled.value)
    assertFalse(RankingsPreferencesRepository(storage).regionalEnabled.value)
  }
}
