/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.mapper.aggregateTeamInfo
import dev.staticvar.vlr.data.mapper.toCompletedMatchEntities
import dev.staticvar.vlr.data.mapper.toRosterEntities
import dev.staticvar.vlr.data.mapper.toTeamEntity
import dev.staticvar.vlr.data.mapper.toTeamPreview
import dev.staticvar.vlr.data.mapper.toUpcomingMatchEntities
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.localsource.database.GetTeamWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.Team_completed_matches
import dev.staticvar.vlr.localsource.database.Team_roster
import dev.staticvar.vlr.localsource.database.Team_upcoming_matches
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.team.TeamDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class TeamRepositoryImpl(
  private val teamDataSource: TeamDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : TeamRepository {

  private val teamsQueries = database.teamsQueries

  override fun getTeams(): Flow<List<TeamInfo>> = teamsQueries
    .getTeamsWithFavoriteStatus()
    .asFlow()
    .mapToList(dispatchers.io)
    .map { teams ->
      teams.map { team ->
        team.toTeamPreview(
          roster = teamsQueries.getTeamRoster(team.id).executeAsList(),
          upcomingMatches = teamsQueries.getUpcomingMatches(team.id).executeAsList(),
          completedMatches = teamsQueries.getCompletedMatches(team.id).executeAsList(),
        )
      }
    }

  override fun getTeamDetails(teamId: String): Flow<TeamInfo?> {
    val teamFlow = teamsQueries
      .getTeamWithFavoriteStatus(teamId)
      .asFlow()
      .mapToOneOrNull(dispatchers.io)

    val rosterFlow = teamsQueries
      .getTeamRoster(teamId)
      .asFlow()
      .mapToList(dispatchers.io)

    val upcomingFlow = teamsQueries
      .getUpcomingMatches(teamId)
      .asFlow()
      .mapToList(dispatchers.io)

    val completedFlow = teamsQueries
      .getCompletedMatches(teamId)
      .asFlow()
      .mapToList(dispatchers.io)

    return teamFlow
      .combine(rosterFlow) { team, roster -> TeamDetailSlices(team = team, roster = roster) }
      .combine(upcomingFlow) { slices, upcoming -> slices.copy(upcoming = upcoming) }
      .combine(completedFlow) { slices, completed -> slices.copy(completed = completed) }
      .map { slices ->
        slices.team?.let {
          aggregateTeamInfo(
            team = it,
            roster = slices.roster,
            upcomingMatches = slices.upcoming,
            completedMatches = slices.completed,
          )
        }
      }
  }

  override fun getTeamsByRegion(region: String): Flow<List<TeamInfo>> = teamsQueries
    .getTeamsByRegion(region.ifBlank { null })
    .asFlow()
    .mapToList(dispatchers.io)
    .map { teams ->
      teams.map { team ->
        team.toTeamPreview(
          roster = teamsQueries.getTeamRoster(team.id).executeAsList(),
          upcomingMatches = teamsQueries.getUpcomingMatches(team.id).executeAsList(),
          completedMatches = teamsQueries.getCompletedMatches(team.id).executeAsList(),
        )
      }
    }

  override suspend fun addToFavorites(teamId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      teamsQueries.addFavoriteTeam(teamId)
      Unit
    }
  }

  override suspend fun removeFromFavorites(teamId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      teamsQueries.removeFavoriteTeam(teamId)
      Unit
    }
  }

  override suspend fun refreshTeamDetails(teamId: String): Result<Unit> = withContext(dispatchers.io) {
    teamDataSource.details(teamId).mapCatching { dto ->
      database.transaction {
        val teamEntity = dto.toTeamEntity(id = teamId)
        teamsQueries.insertTeam(teamEntity)

        teamsQueries.deleteTeamRoster(teamId)
        teamsQueries.deleteUpcomingMatches(teamId)
        teamsQueries.deleteCompletedMatches(teamId)

        dto.toRosterEntities(teamId).forEach { member ->
          teamsQueries.insertTeamRosterMemberDetails(
            team_id = member.team_id,
            player_id = member.player_id,
            player_name = member.player_name,
            player_alias = member.player_alias,
            player_image_url = member.player_image_url,
            player_country = member.player_country,
            is_stand_in = member.is_stand_in,
            is_coach = member.is_coach,
            is_current = member.is_current,
            role = member.role,
          )
        }

        dto.toUpcomingMatchEntities(teamId).forEach { match ->
          teamsQueries.insertUpcomingMatchDetails(
            team_id = match.team_id,
            match_id = match.match_id,
            opponent_team_id = match.opponent_team_id,
            opponent_team_name = match.opponent_team_name,
            opponent_team_logo_url = match.opponent_team_logo_url,
            date = match.date,
            eta = match.eta,
            event_name = match.event_name,
            event_logo_url = match.event_logo_url,
            event_id = match.event_id,
            stage = match.stage,
          )
        }

        dto.toCompletedMatchEntities(teamId).forEach { match ->
          teamsQueries.insertCompletedMatchDetails(
            team_id = match.team_id,
            match_id = match.match_id,
            opponent_team_id = match.opponent_team_id,
            opponent_team_name = match.opponent_team_name,
            opponent_team_logo_url = match.opponent_team_logo_url,
            date = match.date,
            event_name = match.event_name,
            event_logo_url = match.event_logo_url,
            event_id = match.event_id,
            stage = match.stage,
            result = match.result,
          )
        }
      }
    }
  }

  private data class TeamDetailSlices(
    val team: GetTeamWithFavoriteStatus?,
    val roster: List<Team_roster> = emptyList(),
    val upcoming: List<Team_upcoming_matches> = emptyList(),
    val completed: List<Team_completed_matches> = emptyList(),
  )
}
