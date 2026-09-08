/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.appearance

import androidx.lifecycle.ViewModel
import dev.staticvar.vlr.core.settings.AppearanceMode
import dev.staticvar.vlr.core.settings.AppearanceRepository
import dev.staticvar.vlr.core.settings.AppearanceSettings
import dev.staticvar.vlr.core.settings.CatppuccinFlavour
import dev.staticvar.vlr.core.settings.MascotPreference
import dev.staticvar.vlr.core.settings.ThemeFamily
import kotlinx.coroutines.flow.StateFlow

internal class AppearanceViewModel(private val repository: AppearanceRepository) : ViewModel() {
  val appearance: StateFlow<AppearanceSettings> = repository.settings

  fun setMode(mode: AppearanceMode) {
    repository.setMode(mode)
  }

  fun setFamily(family: ThemeFamily) {
    repository.setFamily(family)
  }

  fun setCatppuccinFlavour(flavour: CatppuccinFlavour) {
    repository.setCatppuccinFlavour(flavour)
  }

  fun setMascot(mascot: MascotPreference) {
    repository.setMascot(mascot)
  }
}
