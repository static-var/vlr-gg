package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Entity
@Immutable
data class TournamentDetails(
  @PrimaryKey @SerialName("id") val id: String = "", // 890
  @SerialName("dates") val dates: String = "", // Feb 13 - 14, 2022
  @SerialName("img") val img: String = "", // https://owcdn.net/img/62099d2b41b45.png
  @SerialName("location") val location: String = "", // ca
  @SerialName("matches") val matches: List<Games> = listOf(),
  @SerialName("teams") val participants: List<Participant> = listOf(),
  @SerialName("prize") val prize: String = "", // $2,000 CAD~ $1,572 USD
  @SerialName("prizes") val prizes: List<Prize> = listOf(),
  @SerialName("subtitle") val subtitle: String = "",
  @SerialName("title") val title: String = "", // Toronto VALORANT: Viper's Pit $2K Online Qualifier
  @SerialName("status") val status: Status = Status.UNKNOWN,
  @ColumnInfo("markedFav", defaultValue = "0")
  val markedFav: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
) {

  @Keep
  @Serializable
  @Immutable
  data class Games(
    @SerialName("date") val date: String = "", // Sun, February 13, 2022
    @SerialName("eta") val eta: String? = "", // 1d 22h
    @SerialName("id") val id: String = "", // 71222
    @SerialName("round") val round: String = "", // Opening (A)
    @SerialName("stage") val stage: String = "", // Group Stage
    @SerialName("status") val status: String = "", // Completed
    @SerialName("teams") val teams: List<Team> = listOf(),
    @SerialName("time") var time: String = "" // 4:00 AM
  )

  @Keep
  @Serializable
  @Immutable
  data class Participant(
    @SerialName("id") val id: String = "", // 468
    @SerialName("img") val img: String = "", // https://owcdn.net/img/5f0f5c0ea3426.png
    @SerialName("seed") val seed: String? = "",
    @SerialName("name") val team: String = "" // Renegades
  )

  @Keep
  @Serializable
  @Immutable
  data class Prize(
    @SerialName("position") val position: String = "", // 1st
    @SerialName("prize") val prize: String = "", // $1,200CAD
    @SerialName("team") val team: Team? = null
  )

  @Keep
  @Serializable
  @Immutable
  enum class Status {
    @SerialName("completed") COMPLETED,
    @SerialName("ongoing") ONGOING,
    @SerialName("upcoming") UPCOMING,
    @SerialName("unknown") UNKNOWN
  }
}
