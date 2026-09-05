/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Owns appearance preferences. Initial values are read before exposing state to the UI. */
public class AppearanceRepository(private val storage: Settings) {
  private val mutableSettings = MutableStateFlow(
    AppearanceSettings(
      mode = AppearanceMode.entries.firstOrNull { it.name == storage.getStringOrNull(ModeKey) },
      family = ThemeFamily.entries.firstOrNull { it.name == storage.getStringOrNull(FamilyKey) }
        ?: ThemeFamily.Brutalist,
    ),
  )
  public val settings: StateFlow<AppearanceSettings> = mutableSettings.asStateFlow()

  public fun setMode(mode: AppearanceMode) {
    storage.putString(ModeKey, mode.name)
    mutableSettings.update { it.copy(mode = mode) }
  }

  public fun setFamily(family: ThemeFamily) {
    storage.putString(FamilyKey, family.name)
    mutableSettings.update { it.copy(family = family) }
  }

  private companion object {
    const val ModeKey: String = "appearance.mode"
    const val FamilyKey: String = "appearance.family"
  }
}
