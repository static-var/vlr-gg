package dev.unusedvariable.vlr.data.api.response

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Entity
data class MatchPreviewInfo(
    @PrimaryKey @SerialName("id") val id: String = "", // 71754
    @SerialName("event") val event: String = "", // Champions Tour Japan Stage 1: Challengers Week 1
    @SerialName("series") val series: String = "", // Open Qualifier: Group F
    @SerialName("status") val status: String = "", // completed
    @SerialName("team1") val team1: Team = Team(),
    @SerialName("team2") val team2: Team = Team(),
    @SerialName("time") val time: String? = null // 13h
)
