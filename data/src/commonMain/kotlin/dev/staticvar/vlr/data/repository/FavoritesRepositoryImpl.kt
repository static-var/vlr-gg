/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class FavoritesRepositoryImpl(
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : FavoritesRepository {
  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = database.homeQueries
    .getDirectFavorites { entityType, id, title, imageUrl ->
      when (entityType) {
        "TEAM" -> DirectFavorite.Team(id = id, title = title, imageUrl = imageUrl)
        "EVENT" -> DirectFavorite.Event(id = id, title = title, imageUrl = imageUrl)
        "MATCH" -> DirectFavorite.Match(id = id, title = title, imageUrl = imageUrl)
        "PLAYER" -> DirectFavorite.Player(id = id, title = title, imageUrl = imageUrl)
        else -> error("Unknown direct favorite type: $entityType")
      }
    }
    .asFlow()
    .mapToList(dispatchers.io)
    .map { favorites ->
      DirectFavoriteSnapshot(
        teams = favorites.filterIsInstance<DirectFavorite.Team>(),
        events = favorites.filterIsInstance<DirectFavorite.Event>(),
        matches = favorites.filterIsInstance<DirectFavorite.Match>(),
        players = favorites.filterIsInstance<DirectFavorite.Player>(),
      )
    }
    .distinctUntilChanged()

  override fun observeTeamIds(): Flow<Set<String>> = database.teamsQueries
    .getFavoriteTeamIds()
    .asFlow()
    .mapToList(dispatchers.io)
    .map { it.toSet() }
    .distinctUntilChanged()

  override fun observePlayerIds(): Flow<Set<String>> = database.playersQueries
    .getFavoritePlayerIds()
    .asFlow()
    .mapToList(dispatchers.io)
    .map { it.toSet() }
    .distinctUntilChanged()
}
