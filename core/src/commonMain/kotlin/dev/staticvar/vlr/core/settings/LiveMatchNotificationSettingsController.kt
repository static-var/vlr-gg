/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.settings

import dev.staticvar.vlr.core.notifications.NotificationAuthorization
import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Describes platform support, permission status, and any active permission request. */
public data class LiveMatchNotificationAccess(
  val activitiesEnabled: Boolean?,
  val notifications: NotificationAuthorization? = null,
  val requesting: Boolean = false,
  val supportsLiveUpdates: Boolean = true,
  val requiresNotificationPermission: Boolean = true,
)

/** Coordinates the live-update setting with platform permission checks and requests. */
public class LiveMatchNotificationSettingsController(
  private val repository: LiveMatchNotificationPreferencesRepository,
  private val provider: NotificationPermissionProvider,
  private val onAuthorizationChanged: (NotificationAuthorization) -> Unit = {},
) {
  public val preferences: StateFlow<LiveMatchNotificationPreferences> = repository.preferences
  public val access: StateFlow<LiveMatchNotificationAccess>
    field = MutableStateFlow(readAccess())
  private var generation: Int = 0

  /**
   * Refreshes platform capability and permission state without prompting the user.
   * Results from an older permission check cannot overwrite a newer request.
   */
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

  /**
   * Persists the user preference and requests permission only when the platform needs it.
   * Platforms using Live Activity access refresh that access without a notification prompt.
   */
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

  /**
   * Starts one permission request for a supported platform and publishes its result.
   * The request generation prevents late callbacks from replacing newer state.
   */
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
