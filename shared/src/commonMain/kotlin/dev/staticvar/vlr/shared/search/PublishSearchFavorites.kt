/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

@Composable
internal fun PublishSearchFavorites(onFavoritesChanged: suspend (String) -> Unit) {
  val favorites = koinInject<FavoritesRepository>()
  val publish by rememberUpdatedState(onFavoritesChanged)
  LaunchedEffect(favorites) {
    favorites.observeDirectFavorites()
      .map { it.searchFavorites() }
      .distinctUntilChanged()
      .collect { publish(Json.encodeToString(it)) }
  }
}

@Serializable
internal enum class SearchFavoriteKind {
  @SerialName("team")
  Team,
  @SerialName("event")
  Event,
  @SerialName("match")
  Match,
  @SerialName("player")
  Player,
}

@Serializable
internal data class SearchFavorite(
  val id: String,
  val kind: SearchFavoriteKind,
  val sourceId: String,
  val title: String,
)

internal fun DirectFavoriteSnapshot.searchFavorites(): List<SearchFavorite> =
  (teams + events + matches + players).map { favorite ->
    val kind = when (favorite) {
      is DirectFavorite.Team -> SearchFavoriteKind.Team
      is DirectFavorite.Event -> SearchFavoriteKind.Event
      is DirectFavorite.Match -> SearchFavoriteKind.Match
      is DirectFavorite.Player -> SearchFavoriteKind.Player
    }
    SearchFavorite(
      id = "${kind.name.lowercase()}:${favorite.id}",
      kind = kind,
      sourceId = favorite.id,
      title = favorite.title,
    )
  }.distinctBy { it.id }.sortedBy { it.id }
