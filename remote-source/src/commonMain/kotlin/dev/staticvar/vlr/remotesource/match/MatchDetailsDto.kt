/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.common.MatchStatusNullableSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Match details payload returned by /api/v1/matches/{id}.
 * NOTE: The live API (as observed Oct 2025) does not include an explicit top-level "id" field.
 * The model keeps an id property (default empty) for backward compatibility with earlier synthetic
 * fixtures and to avoid deserialization failure when the field is absent.
 */
@Serializable
data class MatchDetailsDto(
  @SerialName("id") val id: String = "",
  @SerialName("event") val event: EventDto = EventDto(id = ""),
  @SerialName("previous_encounters") val head2head: List<PreviousEncounterDto> = emptyList(),
  @SerialName("note") val note: String = "",
  @SerialName("score") val score: String = "",
  @SerialName("teams") val teams: List<TeamDto> = emptyList(),
  @SerialName("bans") val bans: List<String> = emptyList(),
  @SerialName("videos") val videos: MatchVideosDto = MatchVideosDto(),
  @SerialName("data") val matchData: List<MapDataDto> = emptyList(),
  @SerialName("map_count") val mapCount: Int = 0,
)

@Serializable
data class EventDto(
  @SerialName("id") val id: String,
  @SerialName("name") val name: String = "",
  @SerialName("series") val series: String = "",
  @SerialName("stage") val stage: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("date") val date: String? = null,
  @SerialName("patch") val patch: String? = null,
  @SerialName("status")
  @Serializable(with = MatchStatusNullableSerializer::class) val status: MatchStatus? = null,
)

@Serializable
data class PreviousEncounterDto(
  @SerialName("match_id") val id: String = "",
  @SerialName("teams") val teams: List<TeamDto> = emptyList(),
)

@Serializable
data class MatchVideosDto(
  @SerialName("streams") val streams: List<VideoReferenceDto> = emptyList(),
  @SerialName("vods") val vods: List<VideoReferenceDto> = emptyList(),
)

@Serializable
data class VideoReferenceDto(@SerialName("name") val name: String = "", @SerialName("url") val url: String = "")

@Serializable
data class MapDataDto(
  @SerialName("map") val map: String = "",
  @SerialName("members") val members: List<PlayerStatsDto> = emptyList(),
  @SerialName("teams") val teams: List<TeamDto> = emptyList(),
  @SerialName("rounds") val rounds: List<RoundInfoDto> = emptyList(),
)

@Serializable
data class PlayerStatsDto(
  @SerialName("id") val playerId: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("team") val team: String = "",
  @SerialName("acs") val acs: Int = 0,
  @SerialName("adr") val adr: Int = 0,
  @SerialName("kills") val kills: Int = 0,
  @SerialName("deaths") val deaths: Int = 0,
  @SerialName("assists") val assists: Int = 0,
  @SerialName("kast") val kast: Int = 0,
  @SerialName("first_kills") val firstKills: Int = 0,
  @SerialName("first_deaths") val firstDeaths: Int = 0,
  @SerialName("first_kills_diff") val firstKillsDiff: Int = 0,
  @SerialName("headshot_percent") val hsPercent: Int = 0,
  @SerialName("rating") val rating: Float = 0.0f,
  @SerialName("agents") val agents: List<AgentInfoDto> = emptyList(),
)

@Serializable
data class AgentInfoDto(@SerialName("name") val name: String = "", @SerialName("img") val img: String = "")

@Serializable
data class RoundInfoDto(
  @SerialName("round_number") val roundNo: Int = 0,
  @SerialName("round_score") val score: String = "",
  @SerialName("winner") val winner: String = "",
  @SerialName("side") val side: String = "",
  @SerialName("win_type") val winType: String = "",
)
