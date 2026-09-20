/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider
import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public data class LiveMatchNotificationAccess(
  val activitiesEnabled: Boolean?,
  val notifications: NotificationAuthorization? = null,
  val requesting: Boolean = false,
)

public class LiveMatchNotificationSettingsController(
  private val repository: LiveMatchNotificationPreferencesRepository,
  private val provider: NotificationPermissionProvider,
) {
  public val preferences: StateFlow<LiveMatchNotificationPreferences> = repository.preferences
  public val access: StateFlow<LiveMatchNotificationAccess>
    field = MutableStateFlow(LiveMatchNotificationAccess(provider.areLiveActivitiesEnabled()))
  private var generation: Int = 0

  public fun refresh() {
    if (access.value.requesting) return
    val current = ++generation
    access.value = access.value.copy(activitiesEnabled = provider.areLiveActivitiesEnabled())
    provider.readNotificationAuthorization { result ->
      if (generation == current) access.value = access.value.copy(notifications = result)
    }
  }

  public fun setEnabled(enabled: Boolean) {
    repository.setEnabled(enabled)
    if (enabled) {
      when (access.value.notifications) {
        null, NotificationAuthorization.NotDetermined, NotificationAuthorization.Error -> requestNotifications()
        NotificationAuthorization.Authorized, NotificationAuthorization.Denied -> refresh()
      }
    }
  }

  public fun requestNotifications() {
    if (access.value.requesting) return
    val current = ++generation
    access.value = access.value.copy(requesting = true)
    provider.requestNotificationAuthorization { result ->
      if (generation == current) {
        access.value = LiveMatchNotificationAccess(provider.areLiveActivitiesEnabled(), result)
      }
    }
  }

  public fun openSettings() {
    provider.openSettings()
  }
}
