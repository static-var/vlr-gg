/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheCleanupPreferencesRepositoryTest {
  @Test
  fun cleanupRequiresOptInAndKeepsTheChoiceAcrossRestarts() {
    val storage = MapSettings()
    val preferences = CacheCleanupPreferencesRepository(storage)
    assertFalse(preferences.enabled.value)

    preferences.setEnabled(true)
    assertTrue(preferences.enabled.value)
    val restarted = CacheCleanupPreferencesRepository(storage)
    assertTrue(restarted.enabled.value)

    restarted.setEnabled(false)
    assertFalse(restarted.enabled.value)
    assertFalse(CacheCleanupPreferencesRepository(storage).enabled.value)
  }
}
