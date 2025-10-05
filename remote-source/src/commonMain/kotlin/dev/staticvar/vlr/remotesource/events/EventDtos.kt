package dev.staticvar.vlr.remotesource.events

import dev.staticvar.vlr.remotesource.common.EventStatus
import dev.staticvar.vlr.remotesource.common.EventStatusNullableSerializer
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.common.MatchStatusNullableSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventListDto(
  @SerialName("id") val id: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("status") @Serializable(with = EventStatusNullableSerializer::class) val status: EventStatus? = null,
  @SerialName("prize") val prize: String = "",
  @SerialName("dates") val dates: String = "",
  @SerialName("location") val location: String = "",
  @SerialName("img") val img: String = "",
)

@Serializable
data class EventPrizeTeamDto(
  @SerialName("id") val id: String = "",
  @SerialName("name") val name: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("country") val country: String = "",
)

@Serializable
data class EventPrizeDto(
  @SerialName("position") val position: String = "",
  @SerialName("prize") val prize: String = "",
  @SerialName("team") val team: EventPrizeTeamDto? = null,
)

@Serializable
data class EventTeamDto(
  @SerialName("name") val name: String = "",
  @SerialName("id") val id: String = "",
  @SerialName("img") val img: String = "",
  @SerialName("seed") val seed: String? = null,
)

@Serializable
data class EventMatchTeamDto(
  @SerialName("name") val name: String = "",
  @SerialName("region") val region: String = "",
  @SerialName("score") val score: Int? = null,
)

@Serializable
data class EventMatchDto(
  @SerialName("id") val id: String = "",
  @SerialName("time") val time: String = "",
  @SerialName("date") val date: String = "",
  @SerialName("eta") val eta: String? = null,
  @SerialName("status") @Serializable(with = MatchStatusNullableSerializer::class) val status: MatchStatus? = null,
  @SerialName("teams") val teams: List<EventMatchTeamDto> = emptyList(),
  @SerialName("round") val round: String = "",
  @SerialName("stage") val stage: String = "",
)

@Serializable
data class EventStandingsEntryDto(
  @SerialName("logo") val logo: String = "",
  @SerialName("team") val team: String = "",
  @SerialName("country") val country: String = "",
  @SerialName("wins") val wins: Int = 0,
  @SerialName("losses") val losses: Int = 0,
  @SerialName("ties") val ties: Int = 0,
  @SerialName("map_difference") val mapDifference: Int = 0,
  @SerialName("round_difference") val roundDifference: Int = 0,
  @SerialName("round_delta") val roundDelta: Int = 0,
  @SerialName("group") val group: String? = null,
)

@Serializable
data class EventDetailsDto(
  @SerialName("id") val id: String = "",
  @SerialName("title") val title: String = "",
  @SerialName("subtitle") val subtitle: String = "",
  @SerialName("dates") val dates: String = "",
  @SerialName("prize") val prize: String = "",
  @SerialName("location") val location: String = "",
  @SerialName("status") @Serializable(with = EventStatusNullableSerializer::class) val status: EventStatus? = null,
  @SerialName("img") val img: String = "",
  @SerialName("prizes") val prizes: List<EventPrizeDto> = emptyList(),
  @SerialName("teams") val teams: List<EventTeamDto> = emptyList(),
  @SerialName("matches") val matches: List<EventMatchDto> = emptyList(),
  @SerialName("standings") val standings: List<EventStandingsEntryDto> = emptyList(),
)
