/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.notifications

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import dev.staticvar.vlr.core.notifications.NotificationPermissionProvider

public val LocalNotificationPermissionProvider: ProvidableCompositionLocal<NotificationPermissionProvider?> =
  compositionLocalOf { null }
