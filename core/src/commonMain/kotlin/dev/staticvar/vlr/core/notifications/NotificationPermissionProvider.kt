/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.notifications

/** Represents the result of checking or requesting notification permission. */
public enum class NotificationAuthorization {
  NotDetermined,
  Authorized,
  Denied,
  Error,
}

/** Exposes platform notification permissions, Live Activity access, and system settings. */
public interface NotificationPermissionProvider {
  public fun supportsLiveUpdates(): Boolean = true

  /** Whether ordinary match-start alerts can be delivered when live updates are unavailable. */
  public fun supportsMatchAlerts(): Boolean = false

  public fun requiresNotificationPermission(): Boolean = true

  public fun areLiveActivitiesEnabled(): Boolean?

  /** Returns Android promotion access, or null when unavailable or not applicable. */
  public fun canPromoteNotifications(): Boolean? = null

  public fun readNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit)

  public fun requestNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit)

  public fun openSettings()

  public fun openPromotionSettings() { openSettings() }
}
