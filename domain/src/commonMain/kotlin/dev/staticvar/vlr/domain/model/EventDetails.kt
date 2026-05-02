/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.model

/**
 * Domain model for detailed event information.
 */
data class EventDetails(
  val id: String,
  val title: String,
  val subtitle: String,
  val status: EventStatus,
  val prize: String,
  val dates: String,
  val region: String,
  val logoUrl: String,
  val prizes: List<EventPrize>,
  val teams: List<EventTeam>,
  val matches: List<EventMatch>,
  val standings: List<EventStanding>,
  val isFavorite: Boolean = false,
)

/**
 * Prize placement for an event.
 */
data class EventPrize(val position: String, val prize: String, val team: EventPrizeTeam?)

/**
 * Team that won a prize.
 */
data class EventPrizeTeam(val id: String?, val name: String, val logoUrl: String, val country: String)

/**
 * Team participating in an event.
 */
data class EventTeam(val id: String?, val name: String, val logoUrl: String, val seed: String?)

/**
 * Match within an event.
 */
data class EventMatch(
  val matchId: String,
  val time: String,
  val date: String,
  val eta: String?,
  val status: String,
  val teams: List<EventMatchTeam>,
  val round: String,
  val stage: String,
)

/**
 * Team in an event match.
 */
data class EventMatchTeam(val name: String, val region: String, val score: Int?)

/**
 * Standings entry for an event.
 */
data class EventStanding(
  val teamName: String,
  val teamLogoUrl: String,
  val teamCountry: String,
  val groupName: String?,
  val wins: Int,
  val losses: Int,
  val ties: Int,
  val mapDifference: Int,
  val roundDifference: Int,
  val roundDelta: Int,
)

/**
 * Event status enum.
 */
enum class EventStatus {
  UPCOMING,
  ONGOING,
  COMPLETED,
  UNKNOWN,
}
