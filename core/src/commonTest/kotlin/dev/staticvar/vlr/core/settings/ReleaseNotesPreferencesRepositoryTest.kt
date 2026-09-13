/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ReleaseNotesPreferencesRepositoryTest {
  @Test
  fun acknowledgementSurvivesRestartAndDoesNotHideTheNextRelease() {
    val storage = MapSettings()
    val preferences = ReleaseNotesPreferencesRepository(storage)
    assertNull(preferences.acknowledgedReleaseId.value)

    preferences.acknowledge("first-release")
    assertEquals("first-release", preferences.acknowledgedReleaseId.value)
    val restarted = ReleaseNotesPreferencesRepository(storage)
    assertEquals("first-release", restarted.acknowledgedReleaseId.value)
    assertNotEquals("next-release", restarted.acknowledgedReleaseId.value)

    restarted.acknowledge("next-release")
    restarted.acknowledge("next-release")
    assertEquals("next-release", ReleaseNotesPreferencesRepository(storage).acknowledgedReleaseId.value)
  }
}
