package dev.unusedvariable.vlr.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
open class MatchData(
    open var matchPageLink: String = "",
    open var team1: String = "",
    open var team2: String = "",
    open var team1Score: String = "-",
    open var team2Score: String = "-",
    open var gameExtraInfo: String = "",
    open var isLive: Boolean = false,
    open var eta: String = "",
    open var date: String = ""
)