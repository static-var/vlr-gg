package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Keep
@Serializable
@Immutable
@Entity
data class TeamDetails(
  @PrimaryKey @SerialName("id") var id: String = "", // United States
  @SerialName("country") val country: String = "", // United States
  @JsonNames("logo")
  @SerialName("img")
  val img: String = "", // https://owcdn.net/img/603c00d5c5a08.png
  @SerialName("name") val name: String = "", // 100 Thieves
  @SerialName("rank") val rank: Int = 0, // 8
  @SerialName("region") var region: String = "", // North America
  @SerialName("roster") val roster: List<Roster> = listOf(),
  @SerialName("tag") val tag: String? = "", // 100T
  @SerialName("twitter") val twitter: String? = "", // @100Thieves
  @SerialName("upcoming") val upcoming: List<Games> = listOf(),
  @SerialName("completed") val completed: List<Games> = listOf(),
  @SerialName("website") val website: String? = "", // https://www.100thieves.com/
  @SerialName("points") val points: Int? = null, // true
  val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo("markedFav", defaultValue = "0")
  val markedFav: Boolean = false,
) {
  @Keep
  @Serializable
  @Immutable
  data class Games(
    @SerialName("date") val date: String = "", // 2022-03-07T02:30:00
    @SerialName("event") val event: String = "", // VCT NA S1: Challengers
    @SerialName("id") val id: String = "", // 70074
    @SerialName("opponent") val opponent: String = "", // XSET
    @SerialName("score") val score: String = "", // 1:2
    @SerialName("stage") val stage: String = "", // Group Stage ⋅W4
    @SerialName("eta") val eta: String? = null, // 5d 12h
  )

  @Keep
  @Serializable
  @Immutable
  data class Roster(
    @SerialName("alias") val alias: String = "", // Ethan
    @SerialName("id") val id: String = "", // 11225
    @SerialName("img") val img: String = "", // https://owcdn.net/img/6224af0f3fbd0.png
    @SerialName("name") val name: String? = "", // Ethan Arnold
    @SerialName("role") val role: String? = null, // Team Captain
  )
}
