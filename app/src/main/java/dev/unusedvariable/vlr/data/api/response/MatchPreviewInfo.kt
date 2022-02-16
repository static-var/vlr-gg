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
    @PrimaryKey
    @SerialName("id")
    val id: String = "", // 69995
    @SerialName("date")
    val date: String = "", // Tue, February 15, 2022
    @SerialName("event")
    val event: Event = Event(),
    @SerialName("link")
    val link: String = "", // /69995/surreal-esports-vs-fenerbah-e-esports-vrl-turkey-birlik-week-1
    @SerialName("status")
    val status: String = "", // LIVE
    @SerialName("team1")
    val team1: Team = Team(),
    @SerialName("team2")
    val team2: Team = Team(),
    @SerialName("time")
    val time: String = "" // 10:30 PM
)