/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class RankingsPreferencesRepository(private val storage: Settings) {
  public val regionalEnabled: StateFlow<Boolean>
    field = MutableStateFlow(storage.getBoolean(RegionalEnabledKey, false))

  public fun setRegionalEnabled(enabled: Boolean) {
    storage.putBoolean(RegionalEnabledKey, enabled)
    regionalEnabled.value = enabled
  }

  private companion object {
    const val RegionalEnabledKey: String = "rankings.regional_enabled"
  }
}
