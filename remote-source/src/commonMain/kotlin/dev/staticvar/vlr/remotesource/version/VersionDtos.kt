/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.version

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VersionResponseDto(
  @SerialName("event_list") val eventList: Int = 0,
  @SerialName("event_details") val eventDetails: Int = 0,
  @SerialName("match_list") val matchList: Int = 0,
  @SerialName("match_details") val matchDetails: Int = 0,
  @SerialName("news_list") val newsList: Int = 0,
  @SerialName("player_details") val playerDetails: Int = 0,
  @SerialName("rankings_list") val rankingsList: Int = 0,
  @SerialName("team_details") val teamDetails: Int = 0,
)
