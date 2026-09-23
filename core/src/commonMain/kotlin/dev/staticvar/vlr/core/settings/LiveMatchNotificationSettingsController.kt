/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

public data class LiveMatchNotificationAccess(
  val activitiesEnabled: Boolean?,
  val notifications: NotificationAuthorization? = null,
  val requesting: Boolean = false,
  val supportsLiveUpdates: Boolean = true,
  val requiresNotificationPermission: Boolean = true,
)

public class LiveMatchNotificationSettingsController(
  private val repository: LiveMatchNotificationPreferencesRepository,
  private val provider: NotificationPermissionProvider,
  private val onAuthorizationChanged: (NotificationAuthorization) -> Unit = {},
) {
  public val preferences: StateFlow<LiveMatchNotificationPreferences> = repository.preferences
  public val access: StateFlow<LiveMatchNotificationAccess>
    field = MutableStateFlow(readAccess())
  private var generation: Int = 0

  public fun refresh() {
    if (access.value.requesting) return
    val current = ++generation
    access.value = readAccess(notifications = access.value.notifications)
    if (!access.value.supportsLiveUpdates || !access.value.requiresNotificationPermission) return
    provider.readNotificationAuthorization { result ->
      if (generation == current) {
        access.value = access.value.copy(notifications = result)
        onAuthorizationChanged(result)
      }
    }
  }

  public fun setEnabled(enabled: Boolean) {
    repository.setEnabled(enabled)
    if (enabled && access.value.supportsLiveUpdates) {
      if (!access.value.requiresNotificationPermission) {
        refresh()
        return
      }
      when (access.value.notifications) {
        null, NotificationAuthorization.NotDetermined, NotificationAuthorization.Error -> requestNotifications()
        NotificationAuthorization.Authorized, NotificationAuthorization.Denied -> refresh()
      }
    }
  }

  public fun requestNotifications() {
    if (access.value.requesting || !access.value.supportsLiveUpdates || !access.value.requiresNotificationPermission) return
    val current = ++generation
    access.value = access.value.copy(requesting = true)
    provider.requestNotificationAuthorization { result ->
      if (generation == current) {
        access.value = readAccess(notifications = result)
        onAuthorizationChanged(result)
      }
    }
  }

  public fun openSettings() {
    if (access.value.supportsLiveUpdates) provider.openSettings()
  }

  private fun readAccess(notifications: NotificationAuthorization? = null): LiveMatchNotificationAccess =
    LiveMatchNotificationAccess(
      activitiesEnabled = provider.areLiveActivitiesEnabled(),
      notifications = notifications,
      supportsLiveUpdates = provider.supportsLiveUpdates(),
      requiresNotificationPermission = provider.requiresNotificationPermission(),
    )
}
