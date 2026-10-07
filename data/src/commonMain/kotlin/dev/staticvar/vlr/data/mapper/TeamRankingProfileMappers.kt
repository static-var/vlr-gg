/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.TeamRankingProfile
import dev.staticvar.vlr.domain.model.TeamRankingRecord
import dev.staticvar.vlr.domain.model.TeamRankingResult
import dev.staticvar.vlr.remotesource.rankings.RankingRecordDto
import dev.staticvar.vlr.remotesource.rankings.TeamCircuitDto
import dev.staticvar.vlr.remotesource.rankings.TeamRankingProfileDto

internal fun TeamRankingProfileDto.toDomain(): TeamRankingProfile = TeamRankingProfile(
  rank = rank,
  regionRank = regionRank,
  circuitRank = circuitRank,
  region = RankingRegion.entries.firstOrNull { it.apiValue == region },
  elo = elo,
  mapElo = mapElo,
  matches = matches.toDomain(),
  maps = maps.toDomain(),
  active = active,
  primaryCircuit = circuits.maxWithOrNull(
    compareBy<TeamCircuitDto> { it.matches }.thenBy { it.lastPlayedOn },
  )?.let { circuit -> RankingCircuit.entries.firstOrNull { it.apiValue == circuit.circuit } },
  form = form.map { it == 'W' },
  recent = recent.map { result ->
    TeamRankingResult(
      matchId = result.matchId,
      playedOn = result.playedOn,
      event = result.event,
      stage = result.stage,
      opponentId = result.opponent.id,
      opponentName = result.opponent.name,
      opponentLogo = result.opponent.logo,
      teamScore = result.teamScore,
      opponentScore = result.opponentScore,
      won = result.won,
    )
  },
)

private fun RankingRecordDto.toDomain(): TeamRankingRecord = TeamRankingRecord(
  played = played,
  wins = wins,
  losses = losses,
  winRate = winRate,
)
