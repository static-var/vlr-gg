/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.player

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerAgentStatsDto(
  @SerialName("name") val name: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("count") val count: Int = 0,
  @SerialName("percent") val percent: Double = 0.0,
  @SerialName("rounds") val rounds: Int = 0,
  @SerialName("rating") val rating: Double = 0.0,
  @SerialName("acs") val acs: Double = 0.0,
  @SerialName("kd") val kd: Double = 0.0,
  @SerialName("adr") val adr: Double = 0.0,
  @SerialName("kast") val kast: Double = 0.0,
  @SerialName("kpr") val kpr: Double = 0.0,
  @SerialName("apr") val apr: Double = 0.0,
  @SerialName("fkpr") val fkpr: Double = 0.0,
  @SerialName("fdpr") val fdpr: Double = 0.0,
  @SerialName("k") val k: Int = 0,
  @SerialName("d") val d: Int = 0,
  @SerialName("a") val a: Int = 0,
  @SerialName("fk") val fk: Int = 0,
  @SerialName("fd") val fd: Int = 0,
)

@Serializable
data class PlayerTeamRefDto(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("img") val img: String = "",
)

@Serializable
data class PlayerDetailsDto(
  @SerialName("name") val name: String = "",
  @SerialName("alias") val alias: String = "",
  @SerialName("twitch") val twitch: String? = null,
  @SerialName("twitter") val twitter: String? = null,
  @SerialName("country") val country: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("agents") val agents: List<PlayerAgentStatsDto> = emptyList(),
  @SerialName("total_winnings") val totalWinnings: Double = 0.0,
  @SerialName("current_team") val currentTeam: PlayerTeamRefDto? = null,
  @SerialName("past_teams") val pastTeams: List<PlayerTeamRefDto> = emptyList(),
)
