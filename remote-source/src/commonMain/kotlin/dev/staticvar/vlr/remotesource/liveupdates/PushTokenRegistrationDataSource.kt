/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import dev.staticvar.vlr.core.notifications.PushPlatform

public interface PushTokenRegistrationDataSource {
  public suspend fun register(clientId: String, platform: PushPlatform, token: String): Boolean
}
