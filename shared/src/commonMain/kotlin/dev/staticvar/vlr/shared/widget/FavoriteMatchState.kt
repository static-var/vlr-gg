/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.domain.model.MatchStatus
import kotlin.time.Duration.Companion.hours

/** The single favorite match a one-match widget shows, in priority order. */
public sealed interface FavoriteMatchState {
  public data class Live(val match: UpcomingWidgetMatch) : FavoriteMatchState
  public data class Upcoming(val match: UpcomingWidgetMatch) : FavoriteMatchState
  public data class Completed(val match: UpcomingWidgetMatch, val next: UpcomingWidgetMatch?) : FavoriteMatchState
  public data object NoMatches : FavoriteMatchState
  public data object NoFavorites : FavoriteMatchState
  public data object NeedsApp : FavoriteMatchState
}

public fun favoriteMatchState(snapshot: UpcomingMatchesSnapshot?, nowEpochMillis: Long): FavoriteMatchState {
  if (snapshot == null) return FavoriteMatchState.NeedsApp
  if (!snapshot.hasFavorites) return FavoriteMatchState.NoFavorites
  snapshot.matches.firstOrNull { it.has(MatchStatus.LIVE) }?.let { return FavoriteMatchState.Live(it) }
  val upcoming = snapshot.matches.firstOrNull { match ->
    match.has(MatchStatus.UPCOMING) && match.startTimeEpochMillis.let { it == null || it > nowEpochMillis }
  }
  val windowStart = nowEpochMillis - CompletedWindowMillis
  val recent = snapshot.results
    .filter { result -> result.startTimeEpochMillis.let { it != null && it <= nowEpochMillis && it > windowStart } }
    .maxByOrNull { it.startTimeEpochMillis ?: Long.MIN_VALUE }
  return when {
    recent != null -> FavoriteMatchState.Completed(recent, upcoming)
    upcoming != null -> FavoriteMatchState.Upcoming(upcoming)
    else -> FavoriteMatchState.NoMatches
  }
}

public enum class MatchSide { Team1, Team2 }

/** The side with the higher series score, or null while scores are missing or level. */
public val UpcomingWidgetMatch.winner: MatchSide?
  get() {
    val first = score1 ?: return null
    val second = score2 ?: return null
    return when {
      first > second -> MatchSide.Team1
      second > first -> MatchSide.Team2
      else -> null
    }
  }

private fun UpcomingWidgetMatch.has(status: MatchStatus) = this.status.equals(status.name, ignoreCase = true)

private val CompletedWindowMillis = 24.hours.inWholeMilliseconds
