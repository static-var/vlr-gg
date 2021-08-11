package dev.unusedvariable.vlr.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CombatStats(
    val acs: String = "",
    val kills: String = "",
    val deaths: String = "",
    val assists: String = "",
    val plusMinus: String = "",
    val adr: String = "",
    val headShot: String = ""
)
