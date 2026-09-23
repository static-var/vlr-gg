/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.notifications

import androidx.compose.runtime.staticCompositionLocalOf
import dev.staticvar.vlr.core.settings.LiveMatchNotificationSettingsController

internal val LocalLiveMatchNotificationSettingsController =
  staticCompositionLocalOf<LiveMatchNotificationSettingsController?> { null }
