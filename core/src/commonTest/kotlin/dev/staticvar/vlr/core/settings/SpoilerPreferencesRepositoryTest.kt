/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpoilerPreferencesRepositoryTest {
  @Test
  fun scoresAreVisibleUntilUserEnablesProtection() {
    assertFalse(SpoilerPreferencesRepository(MapSettings()).enabled.value)
  }

  @Test
  fun togglingUpdatesObserversAndSurvivesRestartInBothDirections() {
    val storage = MapSettings()
    val repository = SpoilerPreferencesRepository(storage)
    val observed = repository.enabled

    repository.toggle()
    assertTrue(observed.value)
    val restarted = SpoilerPreferencesRepository(storage)
    assertTrue(restarted.enabled.value)

    restarted.toggle()
    assertFalse(restarted.enabled.value)
    assertFalse(SpoilerPreferencesRepository(storage).enabled.value)
  }
}
