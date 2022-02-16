package dev.unusedvariable.vlr.data.api.response


import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
@Entity
data class TournamentDetails(
    @PrimaryKey
    @SerialName("id")
    val id: String = "", // 890
    @SerialName("bracket")
    val bracket: List<Bracket> = listOf(),
    @SerialName("dates")
    val dates: String = "", // Feb 13 - 14, 2022
    @SerialName("img")
    val img: String = "", // https://owcdn.net/img/62099d2b41b45.png
    @SerialName("location")
    val location: String = "", // ca
    @SerialName("matches")
    val matches: List<Games> = listOf(),
    @SerialName("participants")
    val participants: List<Participant> = listOf(),
    @SerialName("prize")
    val prize: String = "", // $2,000 CAD~ $1,572 USD
    @SerialName("prizes")
    val prizes: List<Prize> = listOf(),
    @SerialName("subtitle")
    val subtitle: String = "",
    @SerialName("title")
    val title: String = "" // Toronto VALORANT: Viper's Pit $2K Online Qualifier
) {
    @Keep
    @Serializable
    data class Bracket(
        @SerialName("lower")
        val lower: List<Games> = listOf(),
        @SerialName("upper")
        val upper: List<Games> = listOf()
    )

    @Keep
    @Serializable
    data class Games(
        @SerialName("date")
        val date: String = "", // Sun, February 13, 2022
        @SerialName("matches")
        val matches: List<Game> = listOf()
    ) {
        @Keep
        @Serializable
        data class Game(
            @SerialName("eta")
            val eta: String = "", // 1d 22h
            @SerialName("id")
            val id: String = "", // 71222
            @SerialName("round")
            val round: String = "", // Opening (A)
            @SerialName("stage")
            val stage: String = "", // Group Stage
            @SerialName("status")
            val status: String = "", // Completed
            @SerialName("teams")
            val teams: List<Team> = listOf(),
            @SerialName("time")
            val time: String = "" // 4:00 AM
        )
    }

    @Keep
    @Serializable
    data class Participant(
        @SerialName("id")
        val id: String = "", // 468
        @SerialName("img")
        val img: String = "", // https://owcdn.net/img/5f0f5c0ea3426.png
        @SerialName("roster")
        val roster: List<Roster> = listOf(),
        @SerialName("seed")
        val seed: String = "",
        @SerialName("team")
        val team: String = "" // Renegades
    ) {
        @Keep
        @Serializable
        data class Roster(
            @SerialName("country")
            val country: String = "", // ca
            @SerialName("playerID")
            val playerID: String = "", // 68
            @SerialName("playerName")
            val playerName: String = "" // Stronglegs
        )
    }

    @Keep
    @Serializable
    data class Prize(
        @SerialName("position")
        val position: String = "", // 1st
        @SerialName("prize")
        val prize: String = "", // $1,200CAD
        @SerialName("team")
        val team: Team = Team()
    )
}