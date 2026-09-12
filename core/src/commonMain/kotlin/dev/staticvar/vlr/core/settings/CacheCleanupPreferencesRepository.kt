/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public class CacheCleanupPreferencesRepository(private val storage: Settings) {
  public val enabled: StateFlow<Boolean>
    field = MutableStateFlow(storage.getBoolean(EnabledKey, false))

  public fun setEnabled(enabled: Boolean) {
    storage.putBoolean(EnabledKey, enabled)
    this.enabled.value = enabled
  }

  private companion object {
    const val EnabledKey: String = "cache.autoCleanup.enabled"
  }
}
