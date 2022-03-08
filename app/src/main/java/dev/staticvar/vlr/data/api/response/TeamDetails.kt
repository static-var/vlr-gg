package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class TeamDetails(
  @SerialName("country") val country: String = "", // United States
  @SerialName("img") val img: String = "", // https://owcdn.net/img/603c00d5c5a08.png
  @SerialName("name") val name: String = "", // 100 Thieves
  @SerialName("rank") val rank: Int = 0, // 8
  @SerialName("region") val region: String = "", // North America
  @SerialName("roster") val roster: List<Roster> = listOf(),
  @SerialName("tag") val tag: String = "", // 100T
  @SerialName("twitter") val twitter: String? = "", // @100Thieves
  @SerialName("upcoming") val upcoming: List<Games> = listOf(),
  @SerialName("completed") val completed: List<Games> = listOf(),
  @SerialName("website") val website: String? = "" // https://www.100thieves.com/
) {
  @Keep
  @Serializable
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
  data class Roster(
    @SerialName("alias") val alias: String = "", // Ethan
    @SerialName("id") val id: String = "", // 11225
    @SerialName("img") val img: String = "", // https://owcdn.net/img/6224af0f3fbd0.png
    @SerialName("name") val name: String? = "", // Ethan Arnold
    @SerialName("role") val role: String? = null // Team Captain
  )
}
