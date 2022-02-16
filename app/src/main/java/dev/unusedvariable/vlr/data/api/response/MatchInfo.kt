package dev.unusedvariable.vlr.data.api.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Serializable
@Entity
data class MatchInfo(
    @PrimaryKey
    var id: Long = 0,
    @SerialName("data")
    val `data`: List<MatchDetailData> = listOf(),
    @SerialName("event")
    val event: Event = Event(),
    @SerialName("head2head")
    val head2head: List<Head2head> = listOf(),
    @SerialName("note")
    val note: String = "", // SEN ban Breeze; NRG ban Split; SEN pick Fracture; NRG pick Haven; SEN ban Ascent; NRG ban Icebox; Bind remains
    @SerialName("score")
    val score: String = "", // 2:1
    @SerialName("teams")
    val teams: List<Team> = listOf()
) {
    @Keep
    @Serializable
    data class MatchDetailData(
        @SerialName("map")
        val map: String = "", // Fracture
        @SerialName("members")
        val members: List<Member> = listOf(),
        @SerialName("teams")
        val teams: List<Team> = listOf()
    ) {
        @Keep
        @Serializable
        data class Member(
            @SerialName("acs")
            val acs: String = "", // 226
            @SerialName("agents")
            val agents: List<Agent> = listOf(),
            @SerialName("assists")
            val assists: String = "", // 1
            @SerialName("deaths")
            val deaths: String = "", // 18
            @SerialName("HSpercent")
            val hsPercent: String = "", // 127
            @SerialName("kills")
            val kills: String = "", // 20
            @SerialName("name")
            val name: String = "", // dapr
            @SerialName("team")
            val team: String = "" // SEN
        ) {
            @Keep
            @Serializable
            data class Agent(
                @SerialName("img")
                val img: String = "", // https://vlr.gg/img/vlr/game/agents/chamber.png
                @SerialName("name")
                val name: String = "" // chamber
            )
        }
    }

    @Keep
    @Serializable
    data class Head2head(
        @SerialName("event")
        val event: Event = Event(),
        @SerialName("id")
        val id: String = "", // 16996
        @SerialName("teams")
        val teams: List<Team> = listOf()
    )
}