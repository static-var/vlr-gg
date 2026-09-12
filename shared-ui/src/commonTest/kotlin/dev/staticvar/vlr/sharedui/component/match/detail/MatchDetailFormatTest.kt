/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.TeamDetails
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDetailFormatTest {
  @Test
  fun explicitNoteFormatWinsOverOtherSignals() {
    assertEquals(
      "BO5",
      match(note = "Upper final · Best of 5", status = "completed", mapCount = 2).matchDetailFormatLabel(),
    )
    assertEquals("BO3", match(note = "Opening match (bo3)", status = null).matchDetailFormatLabel())
    assertEquals("BO7", match(note = "best-of-7", status = "upcoming", mapCount = 3).matchDetailFormatLabel())
  }

  @Test
  fun completedFullVetoDerivesFormatFromPicksAndRemainingMap() {
    val bans = listOf(
      "FNC ban Corrode",
      "NRG ban Haven",
      "FNC pick Ascent",
      "NRG pick Abyss",
      "FNC ban Sunset",
      "NRG ban Bind",
      "Lotus remains",
    )

    assertEquals("BO3", match(status = "completed", mapCount = 2, bans = bans).matchDetailFormatLabel())
  }

  @Test
  fun completedPlayedMapCountAndFinalScoreDoNotClaimAFormat() {
    val completedSweep = match(status = "completed", mapCount = 2).copy(
      teams = listOf(team("FNC", 0), team("NRG", 2)),
    )

    assertEquals("TBD", completedSweep.matchDetailFormatLabel())
    assertEquals(
      "TBD",
      completedSweep.copy(bans = listOf("FNC pick Ascent", "NRG pick Abyss")).matchDetailFormatLabel(),
    )
  }

  @Test
  fun upcomingAndLiveOddMapCountsRepresentPlannedFormat() {
    assertEquals("BO3", match(status = "UPCOMING", mapCount = 3).matchDetailFormatLabel())
    assertEquals("BO5", match(status = "live", mapCount = 5).matchDetailFormatLabel())
    assertEquals("BO3", match(status = "ongoing", mapCount = 3).matchDetailFormatLabel())
    assertEquals("TBD", match(status = "live", mapCount = 2).matchDetailFormatLabel())
  }

  private fun match(
    note: String = "",
    status: String? = null,
    mapCount: Int = 0,
    bans: List<String> = emptyList(),
  ): MatchDetails = MatchDetails(
    id = "match-1",
    event = EventInfo("event-1", "VCT", "Stage 2", "Upper Final", "", null, null, status),
    head2head = emptyList(),
    note = note,
    score = "",
    teams = emptyList(),
    bans = bans,
    videos = MatchVideos(emptyList(), emptyList()),
    matchData = emptyList(),
    mapCount = mapCount,
  )

  private fun team(name: String, score: Int): TeamDetails =
    TeamDetails(id = name, name = name, region = "", img = "", score = score, isWinner = score > 0)
}
