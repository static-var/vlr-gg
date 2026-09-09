/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Owns appearance preferences. Initial values are read before exposing state to the UI. */
public class AppearanceRepository(private val storage: Settings) {
  public val settings: StateFlow<AppearanceSettings>
    field = MutableStateFlow(
      AppearanceSettings(
        mode = AppearanceMode.entries.firstOrNull { it.name == storage.getStringOrNull(ModeKey) },
        family = ThemeFamily.entries.firstOrNull { it.name == storage.getStringOrNull(FamilyKey) }
          ?: ThemeFamily.Brutalist,
        catppuccinFlavour = CatppuccinFlavour.entries.firstOrNull {
          it.name == storage.getStringOrNull(FlavourKey)
        } ?: CatppuccinFlavour.Frappe,
        mascot = MascotPreference.entries.firstOrNull {
          it.name == storage.getStringOrNull(MascotKey)
        } ?: MascotPreference.Lynx,
        mascotVisitFrequency = MascotVisitFrequency.entries.firstOrNull {
          it.storageValue == storage.getStringOrNull(MascotVisitFrequencyKey)
        } ?: MascotVisitFrequency.Sometimes,
      ),
    )

  public fun setMode(mode: AppearanceMode) {
    storage.putString(ModeKey, mode.name)
    settings.update { it.copy(mode = mode) }
  }

  public fun setFamily(family: ThemeFamily) {
    storage.putString(FamilyKey, family.name)
    settings.update { it.copy(family = family) }
  }

  public fun setCatppuccinFlavour(flavour: CatppuccinFlavour) {
    storage.putString(FlavourKey, flavour.name)
    settings.update { it.copy(catppuccinFlavour = flavour) }
  }

  public fun setMascot(mascot: MascotPreference) {
    storage.putString(MascotKey, mascot.name)
    settings.update { it.copy(mascot = mascot) }
  }

  public fun setMascotVisitFrequency(frequency: MascotVisitFrequency) {
    storage.putString(MascotVisitFrequencyKey, frequency.storageValue)
    settings.update { it.copy(mascotVisitFrequency = frequency) }
  }

  private companion object {
    const val ModeKey: String = "appearance.mode"
    const val FamilyKey: String = "appearance.family"
    const val FlavourKey: String = "appearance.catppuccin_flavour"
    const val MascotKey: String = "appearance.mascot"
    const val MascotVisitFrequencyKey: String = "appearance.mascot_visit_frequency"
  }
}
