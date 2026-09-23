/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

public interface FavoriteLiveUpdateDataSource {
  public suspend fun replace(
    clientId: String,
    teams: List<String>,
    matches: List<String>,
    players: List<String>,
    events: List<String>,
  ): Boolean
}
