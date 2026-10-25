/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** Native widget content and favorite IDs. Credentials stay in each platform's network configuration. */
@Serializable
public data class UpcomingMatchesSnapshot(
  val savedAtEpochMillis: Long,
  val clientId: String = "",
  val hasFavorites: Boolean,
  val matches: List<UpcomingWidgetMatch>,
  val theme: WidgetTheme,
  val favorites: WidgetFavorites = WidgetFavorites(),
  val spoilersHidden: Boolean = false,
  val favoriteTeams: List<WidgetFavoriteTeam> = emptyList(),
  val favoriteEvents: List<WidgetFavoriteEvent> = emptyList(),
  /** Completed favorite matches, newest first. */
  val results: List<UpcomingWidgetMatch> = emptyList(),
)

@Serializable
public data class WidgetFavoriteTeam(
  val id: String,
  val name: String,
  val tag: String? = null,
  val logo: String? = null,
)

internal fun DirectFavoriteSnapshot.widgetFavoriteTeams(): List<WidgetFavoriteTeam> = teams
  .filter { it.id.isNotBlank() }
  .distinctBy { it.id }
  .map { WidgetFavoriteTeam(it.id, it.title, it.shortName?.takeIf(String::isNotBlank), it.imageUrl.takeIf(String::isNotBlank)) }
  .sortedBy { it.id }

@Serializable
public data class WidgetFavoriteEvent(
  val id: String,
  val name: String,
  val logo: String? = null,
)

internal fun DirectFavoriteSnapshot.widgetFavoriteEvents(): List<WidgetFavoriteEvent> = events
  .filter { it.id.isNotBlank() }
  .distinctBy { it.id }
  .map { WidgetFavoriteEvent(it.id, it.title, it.imageUrl.takeIf(String::isNotBlank)) }
  .sortedBy { it.id }

@Serializable
public data class UpcomingWidgetMatch(
  val id: String,
  val event: String,
  val team1: String,
  val team2: String,
  val startTimeEpochMillis: Long? = null,
  val status: String = "UPCOMING",
  val score1: Int? = null,
  val score2: Int? = null,
  val format: String = "",
  val stage: String = "",
  val team1Id: String? = null,
  val team2Id: String? = null,
  val team1Logo: String? = null,
  val team2Logo: String? = null,
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
  teamIds = (teams.map { it.id } + players.mapNotNull { it.currentTeamId })
    .filter { it.isNotBlank() }.distinct().sorted(),
  eventIds = events.map { it.id }.sorted(),
  playerIds = players.map { it.id }.sorted(),
)

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

internal fun favoriteWidgetMatches(
  matches: List<UpcomingWidgetMatch>,
  nowEpochMillis: Long,
  spoilersHidden: Boolean,
): List<UpcomingWidgetMatch> = matches
  .asSequence()
  .filter { it.status == MatchStatus.UPCOMING.name || it.status == MatchStatus.LIVE.name }
  .distinctBy(UpcomingWidgetMatch::id)
  .filter { it.status == MatchStatus.LIVE.name || it.startTimeEpochMillis == null || it.startTimeEpochMillis > nowEpochMillis }
  .map { if (it.status == MatchStatus.LIVE.name && !spoilersHidden) it else it.withoutScores() }
  .sortedWith(
    compareBy<UpcomingWidgetMatch> { it.status != MatchStatus.LIVE.name }
      .thenBy { it.startTimeEpochMillis ?: Long.MAX_VALUE }
      .thenBy { it.id },
  )
  .toList()

internal fun favoriteWidgetResults(matches: List<UpcomingWidgetMatch>, spoilersHidden: Boolean): List<UpcomingWidgetMatch> = matches
  .asSequence()
  .filter { it.status == MatchStatus.COMPLETED.name }
  .distinctBy(UpcomingWidgetMatch::id)
  .sortedWith(compareByDescending<UpcomingWidgetMatch> { it.startTimeEpochMillis ?: Long.MIN_VALUE }.thenBy { it.id })
  .take(WidgetResultLimit)
  .map { if (spoilersHidden) it.withoutScores() else it }
  .toList()

private const val WidgetResultLimit = 5

private fun UpcomingWidgetMatch.withoutScores() = copy(score1 = null, score2 = null)

/** Team IDs and logos come from [remote], the server row for the same match, matched by team name. */
internal fun FavoriteScheduledMatch.toWidgetMatch(remote: MatchPreview? = null): UpcomingWidgetMatch {
  val side1 = remote?.side(team1)
  val side2 = remote?.side(team2)
  return UpcomingWidgetMatch(
    id = id,
    event = event,
    team1 = team1,
    team2 = team2,
    startTimeEpochMillis = time.epochMillis(),
    status = status.name,
    score1 = score1,
    score2 = score2,
    format = format,
    stage = stage,
    team1Id = side1?.widgetId,
    team2Id = side2?.widgetId,
    team1Logo = side1?.widgetLogo,
    team2Logo = side2?.widgetLogo,
  )
}

internal fun MatchPreview.toWidgetMatch(): UpcomingWidgetMatch = UpcomingWidgetMatch(
  id = id,
  event = event,
  team1 = team1.name,
  team2 = team2.name,
  startTimeEpochMillis = time.epochMillis(),
  status = status.name,
  score1 = team1.score,
  score2 = team2.score,
  format = series.replace(" ", "").uppercase().takeIf { it.matches(Regex("BO[135]")) }.orEmpty(),
  team1Id = team1.widgetId,
  team2Id = team2.widgetId,
  team1Logo = team1.widgetLogo,
  team2Logo = team2.widgetLogo,
)

private fun MatchPreview.side(name: String): TeamPreview? =
  listOf(team1, team2).firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }

private val TeamPreview.widgetId: String? get() = id?.takeIf(String::isNotBlank)
private val TeamPreview.widgetLogo: String? get() = img.takeIf(String::isNotBlank)

private fun String?.epochMillis(): Long? = this?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
