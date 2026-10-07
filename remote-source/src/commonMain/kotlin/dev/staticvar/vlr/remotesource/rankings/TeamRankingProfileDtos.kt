/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamRankingProfileDto(
  @SerialName("team") val team: RankingTeamDto,
  @SerialName("rank") val rank: Int?,
  @SerialName("circuit_rank") val circuitRank: Int?,
  @SerialName("region") val region: String? = null,
  @SerialName("region_rank") val regionRank: Int? = null,
  @SerialName("elo") val elo: Double,
  @SerialName("map_elo") val mapElo: Double,
  @SerialName("matches") val matches: RankingRecordDto,
  @SerialName("maps") val maps: RankingRecordDto,
  @SerialName("first_played_on") val firstPlayedOn: String?,
  @SerialName("last_played_on") val lastPlayedOn: String?,
  @SerialName("active") val active: Boolean,
  @SerialName("circuits") val circuits: List<TeamCircuitDto>,
  @SerialName("form") val form: String,
  @SerialName("recent") val recent: List<TeamRankingResultDto>,
)

@Serializable
data class TeamCircuitDto(
  @SerialName("circuit") val circuit: String,
  @SerialName("matches") val matches: Int,
  @SerialName("last_played_on") val lastPlayedOn: String,
)

@Serializable
data class TeamRankingResultDto(
  @SerialName("match_id") val matchId: String,
  @SerialName("played_on") val playedOn: String,
  @SerialName("event") val event: String,
  @SerialName("stage") val stage: String? = null,
  @SerialName("opponent") val opponent: RankingTeamDto,
  @SerialName("team_score") val teamScore: Int? = null,
  @SerialName("opponent_score") val opponentScore: Int? = null,
  @SerialName("won") val won: Boolean,
)
