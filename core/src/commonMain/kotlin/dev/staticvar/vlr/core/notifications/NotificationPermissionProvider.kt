/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.notifications

public enum class NotificationAuthorization {
  NotDetermined,
  Authorized,
  Denied,
  Error,
}

public interface NotificationPermissionProvider {
  public fun supportsLiveUpdates(): Boolean = true

  public fun requiresNotificationPermission(): Boolean = true

  public fun areLiveActivitiesEnabled(): Boolean?

  public fun readNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit)

  public fun requestNotificationAuthorization(onResult: (NotificationAuthorization) -> Unit)

  public fun openSettings()
}
