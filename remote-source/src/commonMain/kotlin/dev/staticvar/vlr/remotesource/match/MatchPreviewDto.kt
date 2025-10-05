package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.common.MatchStatusNullableSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchPreviewDto(
  @SerialName("id") val id: String = "",
  @SerialName("event") val event: String = "",
  @SerialName("series") val series: String = "",
  @SerialName("status") @Serializable(with = MatchStatusNullableSerializer::class) val status: MatchStatus? = null,
  @SerialName("team1") val team1: TeamDto = TeamDto(name = ""),
  @SerialName("team2") val team2: TeamDto = TeamDto(name = ""),
  @SerialName("time") val time: String? = null,
  @SerialName("event_id") val eventId: String = "",
)

@Serializable
data class TeamDto(
  @SerialName("id") val id: String? = null,
  @SerialName("name") val name: String,
  @SerialName("country") val region: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("score") val score: Int? = null,
  @SerialName("winner") val winner: Boolean? = null,
)
