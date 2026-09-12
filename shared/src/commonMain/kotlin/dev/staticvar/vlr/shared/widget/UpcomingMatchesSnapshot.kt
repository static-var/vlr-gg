/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.MatchStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Native widget content and favorite IDs. Credentials stay in each platform's network configuration. */
@Serializable
public data class UpcomingMatchesSnapshot(
  val savedAtEpochMillis: Long,
  val hasFavorites: Boolean,
  val matches: List<UpcomingWidgetMatch>,
  val theme: WidgetTheme,
  val favorites: WidgetFavorites = WidgetFavorites(),
  val spoilersHidden: Boolean = false,
)

@Serializable
public data class UpcomingWidgetMatch(
  val id: String,
  val event: String,
  val team1: String,
  val team2: String,
  val startTimeEpochMillis: Long?,
  val status: String = "UPCOMING",
  val score1: Int? = null,
  val score2: Int? = null,
  val format: String = "",
  val stage: String = "",
)

@Serializable
public data class WidgetFavorites(
  val matchIds: List<String> = emptyList(),
  val teamIds: List<String> = emptyList(),
  val eventIds: List<String> = emptyList(),
  val playerIds: List<String> = emptyList(),
)

internal fun DirectFavoriteSnapshot.widgetFavorites(): WidgetFavorites = WidgetFavorites(
  matchIds = matches.map { it.id }.sorted(),
  teamIds = teams.map { it.id }.sorted(),
  eventIds = events.map { it.id }.sorted(),
  playerIds = players.map { it.id }.sorted(),
)

internal val widgetJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

/** Colors are unsigned 32-bit ARGB values, usable without loading Compose in a widget. */
@Serializable
public data class WidgetTheme(
  val background: Long,
  val surface: Long,
  val accent: Long,
  val content: Long,
  val secondary: Long,
  val border: Long,
  val monospace: Boolean,
)

internal fun favoriteWidgetMatches(matches: List<FavoriteScheduledMatch>, nowEpochMillis: Long, spoilersHidden: Boolean): List<UpcomingWidgetMatch> = matches
  .asSequence()
  .filter { it.status == MatchStatus.UPCOMING || it.status == MatchStatus.LIVE }
  .distinctBy(FavoriteScheduledMatch::id)
  .map { match ->
    UpcomingWidgetMatch(
      id = match.id,
      event = match.event,
      team1 = match.team1,
      team2 = match.team2,
      startTimeEpochMillis = match.time?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() },
      status = match.status.name,
      score1 = match.score1.takeUnless { spoilersHidden || match.status != MatchStatus.LIVE },
      score2 = match.score2.takeUnless { spoilersHidden || match.status != MatchStatus.LIVE },
      format = match.format,
      stage = match.stage,
    )
  }
  .filter { it.status == "LIVE" || it.startTimeEpochMillis == null || it.startTimeEpochMillis > nowEpochMillis }
  .sortedWith(compareBy<UpcomingWidgetMatch> { it.status != "LIVE" }.thenBy { it.startTimeEpochMillis ?: Long.MAX_VALUE }.thenBy { it.id })
  .toList()
