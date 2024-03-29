package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Keep
@Serializable
@Immutable
data class Event(
  @SerialName("date") val date: String? = "", // 2021/04/25
  @SerialName("img")
  @JsonNames("icon")
  val img: String = "", // https://owcdn.net/img/6009f963577f4.png
  @SerialName("series") val series: String = "", // VCT NA S2: Challengers 2
  @SerialName("stage") val stage: String = "", // LBF
  @SerialName("id") val id: String = "", // 799
  @SerialName("name") val name: String = "", // 799
  @SerialName("patch") val patch: String? = "", // "Patch 4.02"
  @SerialName("status") val status: String? = "", // "Patch 4.02"
)
