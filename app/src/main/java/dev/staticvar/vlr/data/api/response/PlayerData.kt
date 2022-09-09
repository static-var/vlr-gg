package dev.staticvar.vlr.data.api.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Keep
@Serializable
@Entity
data class PlayerData(
  @PrimaryKey
  @SerialName("id")
  var id: String = "",
  @SerialName("name")
  val name: String = "", // Tyson Ngo
  @SerialName("alias")
  val alias: String = "", // TenZ
  @SerialName("twitch")
  val twitch: String? = "", // https://www.twitch.tv/tenz
  @SerialName("twitter")
  val twitter: String? = "", // @TenZOfficial
  @SerialName("country")
  val country: String? = "", // CANADA
  @SerialName("img")
  val img: String = "", // https://www.vlr.gg/img/base/ph/sil.png
  @SerialName("agents")
  val agents: List<Agent> = listOf(),
  @SerialName("current_team")
  val currentTeam: Team? = null,
  @SerialName("past_teams")
  val previousTeams: List<Team> = listOf(),
  @SerialName("total_winnings")
  val earnings: Double = 0.0,
  val createdAt: Long = System.currentTimeMillis(),
) {
  @Keep
  @Serializable
  @Immutable
  data class Agent(
    @SerialName("name")
    val name: String = "", // jett
    @SerialName("img")
    val img: String = "", // https://www.vlr.gg/img/vlr/game/agents/jett.png
    @SerialName("count")
    val count: Int = 0, // 197
    @SerialName("percent")
    val percent: Double = 0.0, // 63
    @SerialName("rounds")
    val rounds: Int = 0, // 4090
    @SerialName("rating")
    val rating: Double = 0.0, // 1.27
    @SerialName("acs")
    val acs: Double = 0.0, // 267.9
    @SerialName("kd")
    val kd: Double = 0.0, // 1.33
    @SerialName("adr")
    val adr: Double = 0.0, // 159.2
    @SerialName("kast")
    val kast: Double = 0.0, // 72
    @SerialName("kpr")
    val kpr: Double = 0.0, // 0.93
    @SerialName("apr")
    val apr: Double = 0.0, // 0.2
    @SerialName("fkpr")
    val fkpr: Double = 0.0, // 0.21
    @SerialName("fdpr")
    val fdpr: Double = 0.0, // 0.14
    @SerialName("k")
    val k: Int = 0, // 3817
    @SerialName("d")
    val d: Int = 0, // 2864
    @SerialName("a")
    val a: Int = 0, // 820
    @SerialName("fk")
    val fk: Int = 0, // 866
    @SerialName("fd")
    val fd: Int = 0 // 469
  )
}