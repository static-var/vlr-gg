/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.standings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamStandingDto(
  @SerialName("name") val name: String = "",
  @SerialName("id") val id: Int = 0,
  @SerialName("logo") val logo: String = "",
  @SerialName("rank") val rank: Int = 0,
  @SerialName("points") val points: Int = 0,
  @SerialName("country") val country: String = "",
)

@Serializable
data class CircuitStandingDto(
  @SerialName("region") val region: String = "",
  @SerialName("teams") val teams: List<TeamStandingDto> = emptyList(),
)

@Serializable
data class StandingsDto(
  @SerialName("year") val year: Int = 0,
  @SerialName("circuits") val circuits: List<CircuitStandingDto> = emptyList(),
)
