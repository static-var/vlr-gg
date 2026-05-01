package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.data.EventMatches
import dev.staticvar.vlr.data.EventPrizes
import dev.staticvar.vlr.data.EventStandings
import dev.staticvar.vlr.data.EventTeams
import dev.staticvar.vlr.data.Matches
import dev.staticvar.vlr.data.Players
import dev.staticvar.vlr.localsource.database.Events
import dev.staticvar.vlr.data.Teams
import dev.staticvar.vlr.localsource.database.Team_completed_matches
import dev.staticvar.vlr.localsource.database.Team_roster
import dev.staticvar.vlr.localsource.database.Team_upcoming_matches
import dev.staticvar.vlr.remotesource.common.EventStatus
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.events.EventDetailsDto
import dev.staticvar.vlr.remotesource.events.EventListDto
import dev.staticvar.vlr.remotesource.events.EventMatchDto
import dev.staticvar.vlr.remotesource.events.EventMatchTeamDto
import dev.staticvar.vlr.remotesource.events.EventPrizeDto
import dev.staticvar.vlr.remotesource.events.EventStandingsEntryDto
import dev.staticvar.vlr.remotesource.events.EventTeamDto
import dev.staticvar.vlr.remotesource.player.PlayerDetailsDto
import dev.staticvar.vlr.remotesource.team.CompletedMatchDto
import dev.staticvar.vlr.remotesource.team.TeamDetailsDto
import dev.staticvar.vlr.remotesource.team.TeamPlayerDto
import dev.staticvar.vlr.remotesource.team.UpcomingMatchDto
import kotlin.time.Clock

// ----------------------------- Event Mapping -----------------------------

/**
 * Maps EventListDto (list view) to Events entity.
 */
internal fun EventListDto.toEntity(now: Long = Clock.System.now().toEpochMilliseconds()): Events =
  Events(
    id = id,
    name = title,
    subtitle = "",
    status = (status ?: EventStatus.UPCOMING).name,
    prizes = prize,
    dates = dates,
    region = location.ifBlank { null },
    logo_url = img,
    last_updated = now
  )

/**
 * Maps EventDetailsDto (detail view) to Events entity.
 */
internal fun EventDetailsDto.toEventEntity(now: Long = Clock.System.now().toEpochMilliseconds()): Events = Events(
  id = id,
  name = title,
  subtitle = subtitle,
  status = (status ?: EventStatus.UPCOMING).name, // default to UPCOMING when null
  prizes = prize,
  dates = dates,
  region = location.ifBlank { null },
  logo_url = img,
  last_updated = now,
)

internal fun EventDetailsDto.toPrizeEntities(): List<EventPrizes> =
  prizes.map { it.toPrizeEntity(eventId = id) }

internal fun EventPrizeDto.toPrizeEntity(eventId: String): EventPrizes =
  EventPrizes(
    id = 0, // AUTOINCREMENT dummy placeholder ignored on insert (SqlDelight will handle)
    event_id = eventId,
    position = position,
    prize = prize,
    team_id = team?.id?.takeIf { it.isNotBlank() },
    team_name = team?.name ?: "",
    team_logo_url = team?.img ?: "",
    team_country = team?.country ?: "",
  )

internal fun EventDetailsDto.toTeamEntities(): List<EventTeams> =
  teams.map { it.toTeamEntity(eventId = id) }

internal fun EventTeamDto.toTeamEntity(eventId: String): EventTeams =
  EventTeams(
    id = 0,
    event_id = eventId,
    team_id = id.takeIf { it.isNotBlank() },
    team_name = name,
    team_logo_url = img,
    seed = seed,
  )

internal fun EventDetailsDto.toStandingEntities(): List<EventStandings> =
  standings.map { it.toEntity(eventId = id) }

private fun EventStandingsEntryDto.toEntity(eventId: String): EventStandings =
  EventStandings(
    id = 0,
    event_id = eventId,
    team_name = team,
    team_logo_url = logo,
    team_country = country,
    group_name = group,
    wins = wins.toLong(),
    losses = losses.toLong(),
    ties = ties.toLong(),
    map_difference = mapDifference.toLong(),
    round_difference = roundDifference.toLong(),
    round_delta = roundDelta.toLong(),
  )

internal fun EventDetailsDto.toEventMatchLinkEntities(): List<EventMatches> =
  matches.mapNotNull { it.toLinkEntity(parentEventId = id) }

private fun EventMatchDto.toLinkEntity(parentEventId: String): EventMatches? =
  id.takeIf { it.isNotBlank() }?.let { matchId ->
    EventMatches(
      id = 0,
      event_id = parentEventId,
      match_id = matchId,
      round = round,
      stage = stage,
    )
  }

// ----------------------------- Team Mapping -----------------------------

internal fun TeamDetailsDto.toTeamEntity(id: String, now: Long = Clock.System.now().toEpochMilliseconds()): Teams = Teams(
  id = id,
  name = name,
  tag = tag,
  logo_url = img,
  region = region.ifBlank { null },
  country = country,
  roster_url = null,
  earnings = null,
  rank = rank.toLong(),
  website = website,
  twitter = twitter,
  last_updated = now,
)

internal fun TeamDetailsDto.toRosterEntities(teamId: String): List<Team_roster> =
  roster.map { it.toEntity(teamId) }

private fun TeamPlayerDto.toEntity(teamId: String): Team_roster =
  Team_roster(
    id = 0,
    team_id = teamId,
    player_id = id.takeIf { it.isNotBlank() } ?: (teamId + alias),
    player_name = alias.ifBlank { name ?: "" },
    player_country = "", // country not exposed in remote team player dto
    is_stand_in = 0,
    is_coach = (role?.contains("coach", ignoreCase = true) == true).let { if (it) 1 else 0 },
    is_current = 1,
    role = role,
  )

internal fun TeamDetailsDto.toUpcomingMatchEntities(teamId: String): List<Team_upcoming_matches> =
  upcoming.mapNotNull { it.toEntity(teamId) }

private fun UpcomingMatchDto.toEntity(teamId: String): Team_upcoming_matches? {
  if (id.isBlank()) return null
  return Team_upcoming_matches(
    id = 0,
    team_id = teamId,
    match_id = id,
    opponent_team_id = null, // not provided
    opponent_team_name = opponent,
    opponent_team_logo_url = "",
    date = date,
    event_name = event,
    event_logo_url = "",
    event_id = null, // not provided in dto
  )
}

internal fun TeamDetailsDto.toCompletedMatchEntities(teamId: String): List<Team_completed_matches> =
  completed.mapNotNull { it.toEntity(teamId) }

private fun CompletedMatchDto.toEntity(teamId: String): Team_completed_matches? {
  if (id.isBlank()) return null
  return Team_completed_matches(
    id = 0,
    team_id = teamId,
    match_id = id,
    opponent_team_id = null,
    opponent_team_name = opponent,
    opponent_team_logo_url = "",
    date = date,
    event_name = event,
    event_logo_url = "",
    event_id = null,
    result = score,
  )
}

// ----------------------------- Player Mapping -----------------------------

internal fun PlayerDetailsDto.toPlayerEntity(id: String, now: Long = Clock.System.now().toEpochMilliseconds()): Players =
  Players(
    id = id,
    name = name,
    alias = alias,
    real_name = null, // Not provided in DTO
    country = country,
    current_team_id = currentTeam?.id?.takeIf { it.isNotBlank() },
    image_url = img,
    twitter_url = twitter,
    twitch_url = twitch,
    total_winnings = totalWinnings,
    last_updated = now
  )

// Agent stats and team history mappers are already in KonvertMappers.kt:
// - PlayerAgentStatsDto.toEntity(playerId: String): PlayerAgentStats
// - PlayerTeamRefDto.toEntity(playerId: String, isCurrent: Boolean?): PlayerTeamHistory

// ----------------------------- Shared Helpers -----------------------------

private fun EventMatchTeamDto.safeScore(): Long = this.score?.toLong() ?: 0L
