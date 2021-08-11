package dev.unusedvariable.vlr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import dev.unusedvariable.vlr.data.Status

@Entity
@JsonClass(generateAdapter = true)
data class MatchDetails(
    @PrimaryKey
    var matchId: String = "",
    var team1: String = "",
    var team2: String = "",
    var team1Url: String = "https://www.vlr.gg/img/vlr/tmp/vlr.png",
    var team2Url: String = "https://www.vlr.gg/img/vlr/tmp/vlr.png",
    var time: String = "",
    var date: String = "",
    var patchInfo: String? = null,
    var pickAndBans: String? = null,
    var live: Boolean = false,
    var eta: String? = null,
    var scoreLine: String = "",
    var matchNotes: String = "",
    var streams: List<Pair<String, String>>? = null,
    var vod: Pair<String, String>? = null,
    var status: Status = Status(),
    var mapInfo: List<Pair<String, String>>? = null,
    var mapData: List<MapData>? = null
)
