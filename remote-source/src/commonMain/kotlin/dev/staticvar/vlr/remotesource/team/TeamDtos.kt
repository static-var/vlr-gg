/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.team

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamPlayerDto(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String? = null,
  @SerialName("alias") val alias: String = "",
  @SerialName("role") val role: String? = null,
  @SerialName("img") val img: String = "",
)

@Serializable
data class UpcomingMatchDto(
  @SerialName("id") val id: String = "",
  @SerialName("event") val event: String = "",
  @SerialName("stage") val stage: String = "",
  @SerialName("opponent") val opponent: String = "",
  @SerialName("date") val date: String = "",
  @SerialName("eta") val eta: String? = null,
)

@Serializable
data class CompletedMatchDto(
  @SerialName("id") val id: String = "",
  @SerialName("event") val event: String = "",
  @SerialName("stage") val stage: String = "",
  @SerialName("opponent") val opponent: String = "",
  @SerialName("date") val date: String = "",
  @SerialName("score") val score: String = "",
)

@Serializable
data class TeamDetailsDto(
  @SerialName("name") val name: String = "",
  @SerialName("tag") val tag: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("website") val website: String? = null,
  @SerialName("twitter") val twitter: String? = null,
  @SerialName("country") val country: String = "",
  @SerialName("rank") val rank: Int = 0,
  @SerialName("region") val region: String = "",
  @SerialName("roster") val roster: List<TeamPlayerDto> = emptyList(),
  @SerialName("upcoming") val upcoming: List<UpcomingMatchDto> = emptyList(),
  @SerialName("completed") val completed: List<CompletedMatchDto> = emptyList(),
)
