/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.detail

import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchVeto
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.TeamDetails
import dev.staticvar.vlr.domain.model.VetoAction
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchDetailFormatTest {
  @Test
  fun explicitNoteFormatWinsOverOtherSignals() {
    assertEquals(
      5,
      match(note = "Upper final · Best of 5", status = "completed", mapCount = 2).matchDetailBestOf(),
    )
    assertEquals(3, match(note = "Opening match (bo3)", status = null).matchDetailBestOf())
    assertEquals(7, match(note = "best-of-7", status = "upcoming", mapCount = 3).matchDetailBestOf())
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

    assertEquals(3, match(status = "completed", mapCount = 2, bans = bans).matchDetailBestOf())
    assertEquals(3, match(status = " FINAL ", mapCount = 2, bans = bans).matchDetailBestOf())
  }

  @Test
  fun completedStructuredVetoDerivesFormatFromPicksAndRemainingMap() {
    val veto = listOf(
      MatchVeto(team = "FNC", action = VetoAction.BAN, map = "Corrode"),
      MatchVeto(team = "NRG", action = VetoAction.BAN, map = "Haven"),
      MatchVeto(team = "FNC", action = VetoAction.PICK, map = "Ascent"),
      MatchVeto(team = "NRG", action = VetoAction.PICK, map = "Abyss"),
      MatchVeto(team = null, action = VetoAction.REMAINS, map = "Lotus"),
    )

    assertEquals(3, match(status = "completed").copy(veto = veto).matchDetailBestOf())
  }

  @Test
  fun completedPlayedMapCountAndFinalScoreDoNotClaimAFormat() {
    val completedSweep = match(status = "completed", mapCount = 2).copy(
      teams = listOf(team("FNC", 0), team("NRG", 2)),
    )

    assertEquals(null, completedSweep.matchDetailBestOf())
    assertEquals(null, match(status = "FINAL", mapCount = 3).matchDetailBestOf())
    assertEquals(
      null,
      completedSweep.copy(bans = listOf("FNC pick Ascent", "NRG pick Abyss")).matchDetailBestOf(),
    )
  }

  @Test
  fun upcomingAndLiveOddMapCountsRepresentPlannedFormat() {
    assertEquals(3, match(status = "UPCOMING", mapCount = 3).matchDetailBestOf())
    assertEquals(3, match(status = " TbD ", mapCount = 3).matchDetailBestOf())
    assertEquals(5, match(status = "live", mapCount = 5).matchDetailBestOf())
    assertEquals(3, match(status = "ongoing", mapCount = 3).matchDetailBestOf())
    assertEquals(null, match(status = "live", mapCount = 2).matchDetailBestOf())
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
