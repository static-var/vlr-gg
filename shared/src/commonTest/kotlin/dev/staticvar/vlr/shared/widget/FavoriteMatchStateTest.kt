/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget

import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.MatchStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FavoriteMatchStateTest {
  private val now = Instant.parse("2026-10-25T12:00:00Z").toEpochMilliseconds()
  private val hour = 60 * 60 * 1000L

  @Test
  fun `missing snapshot asks for the app`() {
    assertEquals(FavoriteMatchState.NeedsApp, favoriteMatchState(null, now))
  }

  @Test
  fun `snapshot without favorites asks for favorites`() {
    val snapshot = snapshot(matches = listOf(upcoming("next", now + hour))).copy(hasFavorites = false)
    assertEquals(FavoriteMatchState.NoFavorites, favoriteMatchState(snapshot, now))
  }

  @Test
  fun `live match wins over a recent result and an upcoming match`() {
    val live = upcoming("live", now - hour).copy(status = "LIVE", score1 = 1, score2 = 0)
    val snapshot = snapshot(
      matches = listOf(upcoming("next", now + hour), live),
      results = listOf(result("final", now - hour)),
    )
    assertEquals(FavoriteMatchState.Live(live), favoriteMatchState(snapshot, now))
  }

  @Test
  fun `recent result shows with the next upcoming match`() {
    val next = upcoming("next", now + hour)
    val newest = result("newest", now - 2 * hour)
    val snapshot = snapshot(
      matches = listOf(upcoming("stale", now - hour), next),
      results = listOf(result("older", now - 5 * hour), newest),
    )
    assertEquals(FavoriteMatchState.Completed(newest, next), favoriteMatchState(snapshot, now))
  }

  @Test
  fun `recent result without an upcoming match has no next row`() {
    val final = result("final", now - hour)
    assertEquals(FavoriteMatchState.Completed(final, null), favoriteMatchState(snapshot(results = listOf(final)), now))
  }

  @Test
  fun `results expire after twenty four hours`() {
    val day = 24 * hour
    val boundary = result("boundary", now - day)
    val inside = result("inside", now - day + 1)
    assertEquals(FavoriteMatchState.NoMatches, favoriteMatchState(snapshot(results = listOf(boundary)), now))
    assertEquals(FavoriteMatchState.Completed(inside, null), favoriteMatchState(snapshot(results = listOf(inside)), now))
  }

  @Test
  fun `future dated or undated results are ignored`() {
    val next = upcoming("next", now + 3 * hour)
    val snapshot = snapshot(
      matches = listOf(next),
      results = listOf(result("future", now + hour), result("undated", null)),
    )
    assertEquals(FavoriteMatchState.Upcoming(next), favoriteMatchState(snapshot, now))
  }

  @Test
  fun `upcoming match shows when nothing is live or recent`() {
    val tbd = upcoming("tbd", null)
    assertEquals(
      FavoriteMatchState.Upcoming(tbd),
      favoriteMatchState(snapshot(matches = listOf(upcoming("started", now), tbd)), now),
    )
  }

  @Test
  fun `no matches when every row is past`() {
    val snapshot = snapshot(matches = listOf(upcoming("started", now - hour)), results = listOf(result("old", now - 30 * hour)))
    assertEquals(FavoriteMatchState.NoMatches, favoriteMatchState(snapshot, now))
  }

  @Test
  fun `spoiler stripped results still show without a winner`() {
    val schedule = FavoriteScheduledMatch(
      id = "final",
      event = "Champions",
      team1 = "Sentinels",
      team2 = "Paper Rex",
      time = "2026-10-25T10:00:00Z",
      status = MatchStatus.COMPLETED,
      score1 = 2,
      score2 = 1,
      format = "BO3",
    )
    val results = favoriteWidgetResults(listOf(schedule.toWidgetMatch()), spoilersHidden = true)
    val state = favoriteMatchState(snapshot(results = results).copy(spoilersHidden = true), now)
    val match = (state as FavoriteMatchState.Completed).match
    assertEquals("final", match.id)
    assertEquals(null, match.winner)
  }

  @Test
  fun `winner is the side with the higher score`() {
    assertEquals(MatchSide.Team1, result("a", now).copy(score1 = 2, score2 = 1).winner)
    assertEquals(MatchSide.Team2, result("b", now).copy(score1 = 0, score2 = 2).winner)
    assertEquals(null, result("c", now).copy(score1 = 1, score2 = 1).winner)
    assertEquals(null, result("d", now).copy(score1 = 2, score2 = null).winner)
  }

  private fun snapshot(matches: List<UpcomingWidgetMatch> = emptyList(), results: List<UpcomingWidgetMatch> = emptyList()) =
    UpcomingMatchesSnapshot(
      savedAtEpochMillis = now,
      hasFavorites = true,
      matches = matches,
      theme = WidgetTheme(0, 0, 0, 0, 0, 0, false),
      results = results,
    )

  private fun upcoming(id: String, start: Long?) =
    UpcomingWidgetMatch(id = id, event = "Champions", team1 = "Sentinels", team2 = "Paper Rex", startTimeEpochMillis = start)

  private fun result(id: String, start: Long?) =
    upcoming(id, start).copy(status = "COMPLETED", score1 = 2, score2 = 1)
}
