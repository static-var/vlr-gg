/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import dev.staticvar.vlr.core.notifications.PushPlatform

/** Registers a client's platform push token for live updates. */
public interface PushTokenRegistrationDataSource {
  public suspend fun register(clientId: String, platform: PushPlatform, token: String, liveUpdates: Boolean): Boolean

  /** Removes the token associated with an inactive client UUID. */
  public suspend fun delete(clientId: String): Boolean
}
