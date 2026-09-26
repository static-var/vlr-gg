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
import dev.staticvar.vlr.domain.repository.TeamRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

@Composable
internal fun PublishSearchFavorites(onFavoritesChanged: suspend (String) -> Unit) {
  val favorites = koinInject<FavoritesRepository>()
  val teams = koinInject<TeamRepository>()
  val publish by rememberUpdatedState(onFavoritesChanged)
  LaunchedEffect(favorites, teams) {
    val attemptedTeamIds = mutableSetOf<String>()
    var published: List<SearchFavorite>? = null
    favorites.observeDirectFavorites()
      .collect { snapshot ->
        val entries = snapshot.searchFavorites()
        if (entries != published) {
          publish(Json.encodeToString(entries))
          published = entries
        }
        val unresolvedTeamIds = snapshot.teams.mapNotNull { it.unresolvedTeamId } +
          snapshot.matches.flatMap { it.unresolvedTeamIds }
        unresolvedTeamIds.forEach { teamId ->
          if (attemptedTeamIds.add(teamId)) {
            launch { teams.refreshTeamDetails(teamId) }
          }
        }
      }
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
  val aliases: List<String> = emptyList(),
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
      aliases = when (favorite) {
        is DirectFavorite.Team -> listOfNotNull(favorite.shortName)
        is DirectFavorite.Match -> favorite.teamShortNames
        else -> emptyList()
      },
    )
  }.distinctBy { it.id }.sortedBy { it.id }
