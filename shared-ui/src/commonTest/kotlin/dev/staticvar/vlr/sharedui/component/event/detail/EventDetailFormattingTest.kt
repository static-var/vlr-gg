/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class EventDetailFormattingTest {
  @Test
  fun pausedAndUnknownStatusesKeepTheirOwnLabels() {
    assertEquals("PAUSED", EventStatus.PAUSED.eventDetailLabel)
    assertEquals("UNKNOWN", EventStatus.UNKNOWN.eventDetailLabel)
  }

  @Test
  fun eventMetaSkipsBlankValues() {
    val event = eventDetails(region = "Americas", dates = "", prize = "$50,000")

    assertEquals("Americas • $50,000", event.eventDetailMeta())
  }

  @Test
  fun matchTitleUsesBothTeamsWhenAvailable() {
    val match = eventMatch(teamNames = listOf("FNATIC", "Gen.G"))

    assertEquals("FNATIC vs Gen.G", match.eventMatchTitle())
  }

  @Test
  fun matchScheduleSkipsEtaWhenMissing() {
    val match = eventMatch(eta = null, date = "Jun 21", time = "18:00")

    assertEquals("Jun 21 18:00", match.eventMatchSchedule())
  }

  @Test
  fun groupsMatchesBySelectedGroupingInFirstSeenOrder() {
    val matches = listOf(
      eventMatch(id = "1", status = "upcoming", round = "Upper Final", stage = "Playoffs"),
      eventMatch(id = "2", status = "completed", round = "Opening", stage = "Group A"),
      eventMatch(id = "3", status = "upcoming", round = "Opening", stage = "Group A"),
    )

    val grouped = matches.groupEventMatches(EventMatchGrouping.Status)

    assertEquals(listOf("Upcoming", "Completed"), grouped.keys.toList())
    assertEquals(listOf("1", "3"), grouped.getValue("Upcoming").map(EventMatch::matchId))
  }

  @Test
  fun heroDateStatUsesFirstDateInRange() {
    assertEquals("Sep 12", "Sep 12 - Oct 5".eventHeroDateStat())
  }

  @Test
  fun heroPrizeStatCompactsMillionDollarPrize() {
    assertEquals("$2.25M", "$2,250,000".eventHeroPrizeStat())
  }

  private fun eventDetails(
    region: String = "Europe",
    dates: String = "Jun 20 - Jun 30",
    prize: String = "$100,000",
  ): EventDetails = EventDetails(
    id = "event-1",
    title = "Masters Toronto",
    subtitle = "International LAN",
    status = EventStatus.ONGOING,
    prize = prize,
    dates = dates,
    region = region,
    logoUrl = "",
    prizes = emptyList(),
    teams = emptyList(),
    matches = emptyList(),
    standings = emptyList(),
  )

  private fun eventMatch(
    id: String = "match-1",
    teamNames: List<String> = listOf("Team Alpha", "Team Beta"),
    status: String = "upcoming",
    round: String = "Opening",
    stage: String = "Group A",
    eta: String? = "in 2d",
    date: String = "Jun 21",
    time: String = "18:00",
  ): EventMatch = EventMatch(
    matchId = id,
    time = time,
    date = date,
    eta = eta,
    status = status,
    teams = teamNames.map { name -> EventMatchTeam(name = name, region = "", score = null) },
    round = round,
    stage = stage,
  )
}
