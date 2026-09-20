/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public data class LiveMatchNotificationPreferences(val enabled: Boolean = false)

public class LiveMatchNotificationPreferencesRepository(private val storage: Settings) {
  public val preferences: StateFlow<LiveMatchNotificationPreferences>
    field = MutableStateFlow(
      LiveMatchNotificationPreferences(storage.getBoolean("notifications.favorites", false)),
    )

  public fun setEnabled(enabled: Boolean) {
    storage.putBoolean("notifications.favorites", enabled)
    preferences.value = LiveMatchNotificationPreferences(enabled)
  }
}
