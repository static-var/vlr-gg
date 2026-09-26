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
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
    publishSearchFavorites(favorites.observeDirectFavorites(), teams::refreshTeamDetails, publish)
  }
}

internal suspend fun publishSearchFavorites(
  snapshots: Flow<DirectFavoriteSnapshot>,
  refreshTeam: suspend (String) -> Result<Unit>,
  publish: suspend (String) -> Unit,
) = coroutineScope {
  val refreshJobs = mutableMapOf<String, Job>()
  var published: List<SearchFavorite>? = null
  snapshots.collect { snapshot ->
    val entries = snapshot.searchFavorites()
    if (entries != published) {
      publish(Json.encodeToString(entries))
      published = entries
    }
    val unresolvedTeamIds = (snapshot.teams.mapNotNull { it.unresolvedTeamId } +
      snapshot.matches.flatMap { it.unresolvedTeamIds }).toSet()
    (refreshJobs.keys - unresolvedTeamIds).forEach { teamId ->
      refreshJobs.remove(teamId)?.cancel()
    }
    unresolvedTeamIds.forEach { teamId ->
      if (teamId !in refreshJobs) {
        refreshJobs[teamId] = launch {
          var retryDelayMillis = 30_000L
          while (true) {
            if (refreshTeam(teamId).isSuccess) break
            delay(retryDelayMillis)
            retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(30 * 60_000L)
          }
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
