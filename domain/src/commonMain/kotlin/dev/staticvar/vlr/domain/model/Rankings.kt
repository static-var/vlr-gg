/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for regional team rankings.
 */
data class RegionalRanking(val region: String, val teams: List<TeamRanking>)

/**
 * Individual team ranking entry.
 */
data class TeamRanking(
  val teamId: String,
  val teamName: String,
  val teamLogo: String,
  val country: String,
  val rank: Int,
  val points: String,
)
