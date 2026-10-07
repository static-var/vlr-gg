/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.remotesource.rankings.RankingRecordDto
import dev.staticvar.vlr.remotesource.rankings.RankingTeamDto
import dev.staticvar.vlr.remotesource.rankings.TeamCircuitDto
import dev.staticvar.vlr.remotesource.rankings.TeamRankingProfileDto
import dev.staticvar.vlr.remotesource.rankings.TeamRankingResultDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamRankingProfileMappersTest {
  @Test
  fun profile_uses_top_level_region_and_preserves_result_order_and_missing_scores() {
    val dto = profile.copy(
      region = "emea",
      form = "WL",
      recent = listOf(
        TeamRankingResultDto(
          "42",
          "2026-10-03",
          "Champions",
          opponent = RankingTeamDto("2", "Sentinels"),
          teamScore = 2,
          opponentScore = 1,
          won = true,
        ),
        TeamRankingResultDto("41", "2026-10-01", "Champions", opponent = RankingTeamDto("3", "Heretics"), won = false),
      ),
    )

    val mapped = dto.toDomain()

    assertEquals(RankingRegion.Emea, mapped.region)
    assertEquals(listOf(true, false), mapped.form)
    assertEquals(listOf("42", "41"), mapped.recent.map { it.matchId })
    assertEquals("Sentinels", mapped.recent.first().opponentName)
    assertEquals(2, mapped.recent.first().teamScore)
    assertNull(mapped.recent.last().teamScore)
    assertNull(mapped.recent.last().opponentScore)
    assertNull(mapped.recent.last().opponentLogo)
    assertEquals(0.7, mapped.matches.winRate)
    assertEquals(0.6, mapped.maps.winRate)
  }

  @Test
  fun primary_circuit_uses_most_matches_then_latest_played_date() {
    val circuits = listOf(
      TeamCircuitDto("vct", 10, "2026-09-01"),
      TeamCircuitDto("offseason", 9, "2026-10-03"),
      TeamCircuitDto("vcl", 10, "2026-09-29"),
    )

    assertEquals(RankingCircuit.Vcl, profile.copy(circuits = circuits).toDomain().primaryCircuit)
    assertEquals(RankingCircuit.Vct, profile.copy(circuits = circuits.dropLast(1)).toDomain().primaryCircuit)
  }

  @Test
  fun profile_keeps_null_ranks_and_missing_region_and_circuit() {
    val mapped = profile.copy(rank = null, circuitRank = null, regionRank = null).toDomain()

    assertNull(mapped.rank)
    assertNull(mapped.circuitRank)
    assertNull(mapped.regionRank)
    assertNull(mapped.region)
    assertNull(mapped.primaryCircuit)
  }

  private val profile = TeamRankingProfileDto(
    team = RankingTeamDto("2593", "FNATIC", region = "Europe"),
    rank = 4,
    circuitRank = 3,
    regionRank = 2,
    elo = 1842.0,
    mapElo = 1796.0,
    matches = RankingRecordDto(wins = 7, losses = 3, played = 10, winRate = 0.7),
    maps = RankingRecordDto(wins = 12, losses = 8, played = 20, winRate = 0.6),
    firstPlayedOn = "2021-03-14",
    lastPlayedOn = "2026-10-03",
    active = true,
    circuits = emptyList(),
    form = "",
    recent = emptyList(),
  )
}
