package dev.unusedvariable.vlr.data.api.response

import androidx.annotation.Keep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Keep
@Serializable
data class Team constructor(
    @SerialName("country")
    @JsonNames("region")
    val region: String = "", // Canada
    @SerialName("id")
    val id: String = "", // 468
    @SerialName("img")
    @JsonNames("team")
    val img: String = "", // https://owcdn.net/img/5f0f5c0ea3426.png
    @SerialName("name")
    val name: String = "", // Renegades
    @SerialName("score")
    val score: String = "", // 10
    @SerialName("winner")
    val winner: Boolean? = null, // true
)