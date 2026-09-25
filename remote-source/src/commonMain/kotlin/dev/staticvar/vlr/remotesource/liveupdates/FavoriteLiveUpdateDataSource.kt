/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.liveupdates

import kotlinx.serialization.Serializable

/** Direct favorite IDs grouped by entity type. */
@Serializable
public data class FavoriteGroups(
  val teams: List<String>,
  val matches: List<String>,
  val players: List<String>,
  val events: List<String>,
)

/** Result of reading the favorites currently stored for a client. */
public sealed interface FavoriteReadResult {
  public data class Found(val favorites: FavoriteGroups) : FavoriteReadResult

  public data object NotRegistered : FavoriteReadResult

  public data object Failure : FavoriteReadResult
}

/** Reads and changes the direct favorites stored for a client. */
public interface FavoriteLiveUpdateDataSource {
  public suspend fun read(clientId: String): FavoriteReadResult

  public suspend fun add(clientId: String, favorites: FavoriteGroups): Boolean

  public suspend fun remove(clientId: String, favorites: FavoriteGroups): Boolean
}
