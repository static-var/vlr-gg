/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Stores live-update preferences for favorite matches and notification display. */
public data class LiveMatchNotificationPreferences(
  val enabled: Boolean = false,
  val showScoreInStatusBar: Boolean = true,
)

/** Persists the live-update preference and exposes changes to observers. */
public class LiveMatchNotificationPreferencesRepository(private val storage: Settings) {
  public val preferences: StateFlow<LiveMatchNotificationPreferences>
    field = MutableStateFlow(
      LiveMatchNotificationPreferences(
        enabled = storage.getBoolean("notifications.favorites", false),
        showScoreInStatusBar = storage.getBoolean("notifications.showScoreInStatusBar", true),
      ),
    )

  public fun setEnabled(enabled: Boolean) {
    storage.putBoolean("notifications.favorites", enabled)
    preferences.value = preferences.value.copy(enabled = enabled)
  }

  public fun setShowScoreInStatusBar(show: Boolean) {
    storage.putBoolean("notifications.showScoreInStatusBar", show)
    preferences.value = preferences.value.copy(showScoreInStatusBar = show)
  }
}
