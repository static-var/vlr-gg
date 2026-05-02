/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for detailed team information.
 */
data class TeamInfo(
  val id: String,
  val name: String,
  val tag: String,
  val logoUrl: String,
  val region: String,
  val country: String,
  val rank: Int,
  val website: String?,
  val twitter: String?,
  val roster: List<TeamPlayer>,
  val upcomingMatches: List<TeamUpcomingMatch>,
  val completedMatches: List<TeamCompletedMatch>,
  val isFavorite: Boolean = false,
)

/**
 * Player in a team roster.
 */
data class TeamPlayer(
  val id: String,
  val name: String,
  val alias: String,
  val role: String?,
  val imageUrl: String,
  val country: String,
  val isStandIn: Boolean,
  val isCoach: Boolean,
  val isCurrent: Boolean,
)

/**
 * Upcoming match for a team.
 */
data class TeamUpcomingMatch(
  val matchId: String,
  val eventName: String,
  val eventLogoUrl: String,
  val eventId: String?,
  val stage: String,
  val opponent: String,
  val opponentLogoUrl: String,
  val date: String,
  val eta: String?,
)

/**
 * Completed match for a team.
 */
data class TeamCompletedMatch(
  val matchId: String,
  val eventName: String,
  val eventLogoUrl: String,
  val eventId: String?,
  val stage: String,
  val opponent: String,
  val opponentLogoUrl: String,
  val date: String,
  val result: String,
)
