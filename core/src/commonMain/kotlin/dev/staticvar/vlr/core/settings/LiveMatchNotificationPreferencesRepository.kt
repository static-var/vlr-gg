/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

public data class LiveMatchNotificationPreferences(
  val favoriteMatches: Boolean = false,
  val favoriteEvents: Boolean = false,
)

public enum class FavoriteNotificationTarget { Matches, Events }

public class LiveMatchNotificationPreferencesRepository(private val storage: Settings) {
  public val preferences: StateFlow<LiveMatchNotificationPreferences>
    field = MutableStateFlow(
      LiveMatchNotificationPreferences(
        favoriteMatches = storage.getBoolean("notifications.favorite_matches", false),
        favoriteEvents = storage.getBoolean("notifications.favorite_events", false),
      ),
    )

  public fun setEnabled(favorite: FavoriteNotificationTarget, enabled: Boolean) {
    when (favorite) {
      FavoriteNotificationTarget.Matches -> {
        storage.putBoolean("notifications.favorite_matches", enabled)
        preferences.update { it.copy(favoriteMatches = enabled) }
      }
      FavoriteNotificationTarget.Events -> {
        storage.putBoolean("notifications.favorite_events", enabled)
        preferences.update { it.copy(favoriteEvents = enabled) }
      }
    }
  }
}
