/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Selects the iOS Live Activity layout. [storageValue] is shared with the widget extension. */
public enum class LiveActivityStyle(public val storageValue: String) {
  Classic("classic"),
  Glance("glance"),
  Scoreboard("scoreboard"),
  ;

  public companion object {
    public fun fromStorage(value: String?): LiveActivityStyle = entries.firstOrNull { it.storageValue == value } ?: Classic
  }
}

/** Stores live-update preferences for favorite matches and notification display. */
public data class LiveMatchNotificationPreferences(
  val enabled: Boolean = false,
  val showScoreInStatusBar: Boolean = true,
  val liveActivityStyle: LiveActivityStyle = LiveActivityStyle.Classic,
)

/** Persists the live-update preference and exposes changes to observers. */
public class LiveMatchNotificationPreferencesRepository(private val storage: Settings) {
  public val preferences: StateFlow<LiveMatchNotificationPreferences>
    field = MutableStateFlow(
      LiveMatchNotificationPreferences(
        enabled = storage.getBoolean("notifications.favorites", false),
        showScoreInStatusBar = storage.getBoolean("notifications.showScoreInStatusBar", true),
        liveActivityStyle = LiveActivityStyle.fromStorage(storage.getStringOrNull(LiveActivityStyleKey)),
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

  /** iOS mirrors this key into the app group so running Live Activities can switch layout. */
  public fun setLiveActivityStyle(style: LiveActivityStyle) {
    storage.putString(LiveActivityStyleKey, style.storageValue)
    preferences.value = preferences.value.copy(liveActivityStyle = style)
  }

  private companion object {
    const val LiveActivityStyleKey = "notifications.liveActivityStyle"
  }
}
