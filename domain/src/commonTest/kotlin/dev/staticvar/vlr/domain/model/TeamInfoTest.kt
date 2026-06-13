/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TeamInfoTest {
  @Test
  fun matchesInNext7DaysIncludesOnlyMatchesInsideWindow() {
    val team =
      teamInfo(
        upcomingMatches =
        listOf(
          upcomingMatch(matchId = "today", date = "2025-06-13"),
          upcomingMatch(matchId = "week", date = "2025-06-20"),
          upcomingMatch(matchId = "future", date = "2025-07-01"),
          upcomingMatch(matchId = "invalid", date = "TBD"),
        ),
      )

    val matches = team.matchesInNext7Days(today = LocalDate(2025, 6, 13))

    assertEquals(listOf("today", "week"), matches.map { it.matchId })
  }

  @Test
  fun matchesInNext7DaysResolvesDisplayDatesAgainstCurrentYear() {
    val team =
      teamInfo(
        upcomingMatches =
        listOf(
          upcomingMatch(matchId = "oct-3", date = "Oct 3"),
          upcomingMatch(matchId = "oct-11", date = "Oct 11"),
        ),
      )

    val matches = team.matchesInNext7Days(today = LocalDate(2025, 10, 1))

    assertEquals(listOf("oct-3"), matches.map { it.matchId })
  }

  @Test
  fun matchesInNext7DaysRollsDisplayDatesIntoNextYear() {
    val team =
      teamInfo(
        upcomingMatches =
        listOf(
          upcomingMatch(matchId = "jan-2", date = "Jan 2"),
          upcomingMatch(matchId = "feb-1", date = "Feb 1"),
        ),
      )

    val matches = team.matchesInNext7Days(today = LocalDate(2025, 12, 30))

    assertEquals(listOf("jan-2"), matches.map { it.matchId })
  }
}

private fun teamInfo(upcomingMatches: List<TeamUpcomingMatch>): TeamInfo = TeamInfo(
  id = "fnc",
  name = "FNATIC",
  tag = "FNC",
  logoUrl = "",
  region = "EMEA",
  country = "EU",
  rank = 1,
  website = null,
  twitter = null,
  roster = emptyList(),
  upcomingMatches = upcomingMatches,
  completedMatches = emptyList(),
)

private fun upcomingMatch(matchId: String, date: String): TeamUpcomingMatch = TeamUpcomingMatch(
  matchId = matchId,
  eventName = "Valorant Champions",
  eventLogoUrl = "",
  eventId = null,
  stage = "Upper Final",
  opponent = "NRG",
  opponentLogoUrl = "",
  date = date,
  eta = null,
)
