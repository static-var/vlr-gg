/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

data class TeamRankingProfile(
  val rank: Int?,
  val regionRank: Int?,
  val circuitRank: Int?,
  val region: RankingRegion?,
  val elo: Double,
  val mapElo: Double,
  val matches: TeamRankingRecord,
  val maps: TeamRankingRecord,
  val active: Boolean,
  val primaryCircuit: RankingCircuit?,
  val form: List<Boolean>,
  val recent: List<TeamRankingResult>,
)

data class TeamRankingRecord(val played: Int, val wins: Int, val losses: Int, val winRate: Double)

data class TeamRankingResult(
  val matchId: String,
  val playedOn: String,
  val event: String,
  val stage: String?,
  val opponentId: String,
  val opponentName: String,
  val opponentLogo: String?,
  val teamScore: Int?,
  val opponentScore: Int?,
  val won: Boolean,
)
