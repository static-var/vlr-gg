package dev.unusedvariable.vlr.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayerData(
    var name: String = "",
    var org: String = "",
    var agent: String = "",
    var combatStats: CombatStats = CombatStats()
)
