package dev.unusedvariable.vlr.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MapData(
    var mapName: String = "",
    var team1: String = "",
    var team2: String = "",
    var team1Score: String = "",
    var team2Score: String = "",
    var isMapComplete: Boolean = false,
    var team1Players: List<PlayerData>? = null,
    var team2Players: List<PlayerData>? = null
)
