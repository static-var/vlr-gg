package dev.staticvar.vlr.data.api.response

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Immutable
data class RankPerRegion(
  @SerialName("region") val region: String = "",
  @SerialName("teams") val teams: List<TeamDetails> = listOf()
)
