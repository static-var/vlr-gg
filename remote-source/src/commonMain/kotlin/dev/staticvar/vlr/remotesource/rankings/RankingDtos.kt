/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Fields the API contract requires have no defaults, so a malformed page fails to
// decode instead of replacing the cached ranking with empty or anonymous teams.

/** One page of `/api/v2/rankings/`. */
@Serializable
data class RankingListDto(
  @SerialName("total") val total: Int,
  @SerialName("teams") val teams: List<TeamRankingDto>,
  @SerialName("as_of") val asOf: String? = null,
  @SerialName("algorithm") val algorithm: String? = null,
  @SerialName("circuit") val circuit: String = "all",
  @SerialName("region") val region: String = "all",
  @SerialName("limit") val limit: Int = 50,
  @SerialName("offset") val offset: Int = 0,
)

@Serializable
data class TeamRankingDto(
  @SerialName("rank") val rank: Int,
  @SerialName("team") val team: RankingTeamDto,
  @SerialName("elo") val elo: Double,
  @SerialName("matches") val matches: RankingRecordDto,
  @SerialName("region") val region: String? = null,
  @SerialName("overall_rank") val overallRank: Int? = null,
  @SerialName("map_elo") val mapElo: Double,
  @SerialName("maps") val maps: RankingRecordDto? = null,
  @SerialName("last_played_on") val lastPlayedOn: String? = null,
  @SerialName("primary_circuit") val primaryCircuit: String? = null,
  @SerialName("circuits") val circuits: List<String> = emptyList(),
)

@Serializable
data class RankingTeamDto(
  @SerialName("id") val id: String,
  @SerialName("name") val name: String,
  @SerialName("logo") val logo: String? = null,
  @SerialName("country") val country: String? = null,
  @SerialName("tag") val tag: String? = null,
  @SerialName("region") val region: String? = null,
)

@Serializable
data class RankingRecordDto(
  @SerialName("wins") val wins: Int,
  @SerialName("losses") val losses: Int,
  @SerialName("played") val played: Int,
  @SerialName("win_rate") val winRate: Double,
)
