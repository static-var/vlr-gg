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
)

@Serializable
data class TeamRankingDto(
  @SerialName("rank") val rank: Int,
  @SerialName("team") val team: RankingTeamDto,
  @SerialName("elo") val elo: Double,
  @SerialName("matches") val matches: RankingRecordDto,
  @SerialName("region") val region: String? = null,
)

@Serializable
data class RankingTeamDto(
  @SerialName("id") val id: String,
  @SerialName("name") val name: String,
  @SerialName("logo") val logo: String? = null,
  @SerialName("country") val country: String? = null,
)

@Serializable
data class RankingRecordDto(
  @SerialName("wins") val wins: Int,
  @SerialName("losses") val losses: Int,
)
