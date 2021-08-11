package dev.unusedvariable.vlr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class UpcomingMatch(
    @PrimaryKey
    var upcomingId: String = "",
    override var team1: String = "",
    override var team2: String = "",
    override var team1Score: String = "-",
    override var team2Score: String = "-",
    override var gameExtraInfo: String = "",
    override var isLive: Boolean = false,
    override var eta: String = "",
    override var date: String = ""
) : MatchData(upcomingId, team1, team2, team1Score, team2Score, gameExtraInfo, isLive, eta)