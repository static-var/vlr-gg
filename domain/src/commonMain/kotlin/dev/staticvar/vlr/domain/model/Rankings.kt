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

/**
 * One team's line in the global Elo ranking.
 */
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
)
