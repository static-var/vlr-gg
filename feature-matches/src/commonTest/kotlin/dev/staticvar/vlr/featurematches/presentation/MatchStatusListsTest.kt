/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MatchStatusListsTest {
  @Test
  fun upcomingMatchStartingExactlyNowStaysAheadOfOverdueMatches() {
    val now = Instant.parse("2025-01-01T12:00:00Z").toEpochMilliseconds()
    val matches = listOf(
      upcoming(id = "overdue", time = "2025-01-01T11:59:59.999Z"),
      upcoming(id = "later", time = "2025-01-01T13:00:00Z"),
      upcoming(id = "now", time = "2025-01-01T12:00:00Z"),
    )

    val lists = matches.groupByStatus(nowEpochMillis = now)

    assertEquals(listOf("now", "later", "overdue"), lists.upcoming.map(MatchPreview::id))
    assertEquals(lists.upcoming, lists[MatchStatusFilter.Upcoming])
  }

  private fun upcoming(id: String, time: String): MatchPreview = MatchPreview(
    id = id,
    event = "Masters",
    series = "Bo3",
    status = MatchStatus.UPCOMING,
    team1 = team(name = "Alpha"),
    team2 = team(name = "Bravo"),
    time = time,
    eventId = "event-1",
  )

  private fun team(name: String): TeamPreview = TeamPreview(
    id = name.lowercase(),
    name = name,
    region = "EMEA",
    img = "",
    score = null,
    isWinner = null,
  )
}
