package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Entity
@Stable
data class MatchInfo(
  @PrimaryKey var id: String = "",
  @SerialName("event") val event: Event = Event(),
  @SerialName("previous_encounters") val head2head: List<Head2head> = listOf(),
  @SerialName("note")
  val note: String =
    "", // SEN ban Breeze; NRG ban Split; SEN pick Fracture; NRG pick Haven; SEN ban Ascent; NRG
  // ban Icebox; Bind remains
  @SerialName("score") val score: String = "", // 2:1
  @SerialName("teams") val teams: List<Team> = listOf(),
  @SerialName("bans") val bans: List<String> = listOf(),
  @SerialName("videos") val videos: Videos = Videos(),
  @SerialName("data") val matchData: List<MatchDetailData> = listOf(),
  @SerialName("map_count") val mapCount: Int = 0,
) {
  @Keep
  @Serializable
  @Immutable
  data class MatchDetailData(
    @SerialName("map") val map: String = "", // Fracture
    @SerialName("members") val members: List<Member> = listOf(),
    @SerialName("teams") val teams: List<Team> = listOf()
  ) {
    @Keep
    @Serializable
    @Immutable
    data class Member(
      @SerialName("acs") val acs: Int = 0, // 226
      @SerialName("adr") val adr: Int = 0, // 226
      @SerialName("agents") val agents: List<Agent> = listOf(),
      @SerialName("assists") val assists: Int = 0, // 1
      @SerialName("deaths") val deaths: Int = 0, // 18
      @SerialName("kast") val kast: Int = 0, // 18
      @SerialName("first_kills") val firstKills: Int = 0, // 18
      @SerialName("first_deaths") val firstDeaths: Int = 0, // 18
      @SerialName("first_kills_diff") val firstKillsDiff: Int = 0, // 18
      @SerialName("headshot_percent") val hsPercent: Int = 0, // 127
      @SerialName("kills") val kills: Int = 0, // 20
      @SerialName("name") val name: String = "", // dapr
      @SerialName("team") val team: String = "" // SEN
    ) {
      @Keep
      @Serializable
      @Immutable
      data class Agent(
        @SerialName("img") val img: String = "", // https://vlr.gg/img/vlr/game/agents/chamber.png
        @SerialName("name") val name: String = "" // chamber
      )
    }
  }

  @Keep
  @Serializable
  @Immutable
  data class Head2head(
    @SerialName("match_id") val id: String = "", // 16996
    @SerialName("teams") val teams: List<Team> = listOf()
  )

  @Keep
  @Serializable
  @Immutable
  data class Videos(
    @SerialName("streams") val streams: List<ReferenceVideo> = listOf(),
    @SerialName("vods") val vods: List<ReferenceVideo> = listOf(),
  ) {

    @Keep
    @Serializable
    @Immutable
    data class ReferenceVideo(
      @SerialName("name") val name: String = "",
      @SerialName("url") val url: String = "",
    )
  }
}
