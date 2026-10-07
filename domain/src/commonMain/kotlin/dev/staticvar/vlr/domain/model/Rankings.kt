/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

enum class RankingRegion(val apiValue: String) {
  Americas("americas"),
  Emea("emea"),
  Pacific("pacific"),
  China("china"),
}

enum class RankingCircuit(val apiValue: String) {
  All("all"),
  Vct("vct"),
  Vcl("vcl"),
  Tier3("t3"),
  GameChangers("gc"),
  Collegiate("collegiate"),
  Offseason("offseason"),
  Other("other"),
}

enum class RankingMetric(val apiValue: String) {
  Elo("elo"),
  MapElo("map_elo"),
  Matches("matches"),
  WinRate("win_rate"),
}

enum class RankingOrder(val apiValue: String) {
  Desc("desc"),
  Asc("asc"),
}

data class RankingsQuery(
  val circuit: RankingCircuit = RankingCircuit.All,
  val region: RankingRegion? = null,
  val minMatches: Int = 5,
  val includeInactive: Boolean = false,
  val metric: RankingMetric = RankingMetric.Elo,
  val order: RankingOrder = RankingOrder.Desc,
) {
  init {
    require(minMatches in 0..1000) { "Minimum series must be between 0 and 1000" }
  }

  val cacheKey: String
    get() = "${circuit.apiValue}|${region?.apiValue ?: "all"}|$minMatches|$includeInactive|${metric.apiValue}|${order.apiValue}|50|0"
}

data class TeamRanking(
  val teamId: String,
  val teamName: String,
  val teamLogo: String,
  val country: String,
  val rank: Int,
  val elo: Double,
  val wins: Int,
  val losses: Int,
  val isFavorite: Boolean = false,
  val region: RankingRegion? = null,
  val mapElo: Double = elo,
  val matchesPlayed: Int = wins + losses,
  val winRate: Double = if (matchesPlayed == 0) 0.0 else wins.toDouble() / matchesPlayed,
  val overallRank: Int? = null,
)
