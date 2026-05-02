/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for detailed player information.
 */
data class PlayerInfo(
  val id: String,
  val name: String,
  val alias: String,
  val realName: String?,
  val country: String,
  val imageUrl: String,
  val twitterUrl: String?,
  val twitchUrl: String?,
  val totalWinnings: Double,
  val currentTeam: PlayerTeam?,
  val pastTeams: List<PlayerTeam>,
  val agentStats: List<PlayerAgentStat>,
  val isFavorite: Boolean = false,
)

/**
 * Team reference for a player.
 */
data class PlayerTeam(val id: String?, val name: String, val logoUrl: String, val isCurrent: Boolean)

/**
 * Agent statistics for a player.
 */
data class PlayerAgentStat(
  val agentName: String,
  val agentImageUrl: String,
  val usageCount: Int,
  val usagePercent: Double,
  val roundsPlayed: Int,
  val rating: Double,
  val acs: Double,
  val kdRatio: Double,
  val adr: Double,
  val kast: Double,
  val kpr: Double,
  val apr: Double,
  val fkpr: Double,
  val fdpr: Double,
  val kills: Int,
  val deaths: Int,
  val assists: Int,
  val firstKills: Int,
  val firstDeaths: Int,
)
