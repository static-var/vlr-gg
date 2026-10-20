/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlin.time.Instant

internal data class RemoteTeam(val id: String?, val name: String, val tag: String?, val logo: String?, val score: Int?)

internal enum class RemoteMatchPhase { Upcoming, Live, Completed, Unknown }

internal data class CurrentMap(val name: String, val number: Int?, val scores: List<Int?>)

internal data class MatchPreview(
  val id: String,
  val event: String,
  val series: String,
  val phase: RemoteMatchPhase,
  val start: Instant?,
  val left: RemoteTeam,
  val right: RemoteTeam,
  val currentMap: CurrentMap?,
)

internal data class RemoteMap(val name: String, val live: Boolean?, val teams: List<RemoteTeam>)

internal data class RemoteVeto(val team: String?, val action: String, val map: String)

internal data class MatchDetails(
  val teams: List<RemoteTeam>,
  val maps: List<RemoteMap>,
  val veto: List<RemoteVeto>,
  val eventStage: String?,
  val currentMap: CurrentMap?,
  val totalMaps: Int?,
  val mapCount: Int?,
)

internal data class TeamProfile(
  val team: RemoteTeam,
  val rank: Int?,
  val region: String?,
  val regionRank: Int?,
  val recent: List<RemoteTeamResult>,
)

internal data class RemoteTeamResult(
  val id: String,
  val day: WidgetDay?,
  val opponent: RemoteTeam,
  val teamScore: Int?,
  val opponentScore: Int?,
  val won: Boolean,
)

internal enum class RemoteEventStatus { Upcoming, Ongoing, Paused, Completed, Unknown }

internal data class EventProfile(
  val id: String,
  val title: String,
  val dates: EventDates,
  val prize: String?,
  val location: String,
  val status: RemoteEventStatus,
  val logo: String?,
  val teams: List<RemoteEventTeam>,
  val matches: List<RemoteEventMatch>,
  val standings: List<RemoteEventStanding>,
  val prizes: List<RemoteEventPrize>,
)

internal data class RemoteEventTeam(val id: String?, val name: String, val logo: String?)

internal data class RemoteEventParticipant(val name: String, val score: Int?)

internal data class RemoteEventMatch(
  val id: String,
  val day: WidgetDay?,
  val start: Instant?,
  val phase: RemoteMatchPhase,
  val teams: List<RemoteEventParticipant>,
  val round: String,
  val stage: String,
)

internal data class RemoteEventStanding(
  val logo: String?,
  val team: String,
  val wins: Int?,
  val losses: Int?,
  val ties: Int?,
  val mapDifference: Int?,
  val group: String?,
)

internal data class RemoteEventPrize(val position: String, val prize: String, val team: RemoteEventTeam?)
