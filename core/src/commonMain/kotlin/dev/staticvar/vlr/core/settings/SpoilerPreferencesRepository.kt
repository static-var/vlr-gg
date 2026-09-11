/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** App-wide score visibility, loaded before the first screen is rendered. */
public class SpoilerPreferencesRepository(private val storage: Settings) {
  public val enabled: StateFlow<Boolean>
    field = MutableStateFlow(storage.getBoolean(EnabledKey, false))

  public fun toggle() {
    val enabled = !this.enabled.value
    storage.putBoolean(EnabledKey, enabled)
    this.enabled.value = enabled
  }

  private companion object {
    const val EnabledKey: String = "spoilers.hidden"
  }
}
