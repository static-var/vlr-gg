/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.refresh.KeyedRefreshLock
import dev.staticvar.vlr.data.PlayerAgentStats
import dev.staticvar.vlr.data.PlayerTeamHistory
import dev.staticvar.vlr.data.mapper.aggregatePlayerInfo
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.data.mapper.toPlayerEntity
import dev.staticvar.vlr.data.mapper.toPlayerInfo
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.localsource.database.GetPlayerWithFavoriteStatus
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.player.PlayerDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class PlayerRepositoryImpl(
  private val playerDataSource: PlayerDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : PlayerRepository {

  private val playersQueries = database.playersQueries
  private val detailRefreshes = KeyedRefreshLock()

  override fun getPlayerInTeam(teamId: String): Flow<List<PlayerInfo?>> = playersQueries
    .getPlayersByTeam(teamId)
    .asFlow()
    .mapToList(dispatchers.io)
    .map { players ->
      players.mapTo(mutableListOf<PlayerInfo?>()) { player ->
        player.toPlayerInfo(
          agentStats = playersQueries.getPlayerAgentStats(player.id).executeAsList(),
          teamHistory = playersQueries.getPlayerTeamHistory(player.id).executeAsList(),
        )
      }
    }

  override fun getPlayerDetails(playerId: String): Flow<PlayerInfo?> {
    val playerFlow = playersQueries
      .getPlayerWithFavoriteStatus(playerId)
      .asFlow()
      .mapToOneOrNull(dispatchers.io)

    val agentStatsFlow = playersQueries
      .getPlayerAgentStats(playerId)
      .asFlow()
      .mapToList(dispatchers.io)

    val teamHistoryFlow = playersQueries
      .getPlayerTeamHistory(playerId)
      .asFlow()
      .mapToList(dispatchers.io)

    return playerFlow
      .combine(agentStatsFlow) { player, agentStats ->
        PlayerDetailSlices(
          player = player,
          agentStats = agentStats,
        )
      }
      .combine(teamHistoryFlow) { slices, teamHistory ->
        slices.copy(teamHistory = teamHistory)
      }
      .map { slices ->
        slices.player?.let { player ->
          aggregatePlayerInfo(
            player = player,
            agentStats = slices.agentStats,
            teamHistory = slices.teamHistory,
          )
        }
      }
  }

  override suspend fun addToFavorites(playerId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      playersQueries.addFavoritePlayer(playerId)
      Unit
    }
  }

  override suspend fun removeFromFavorites(playerId: String): Result<Unit> = withContext(dispatchers.io) {
    runCatching {
      playersQueries.removeFavoritePlayer(playerId)
      Unit
    }
  }

  override suspend fun refreshPlayerDetails(playerId: String): Result<Unit> = withContext(dispatchers.io) {
    detailRefreshes.withLock(playerId) {
      playerDataSource.details(playerId).mapCatching { dto ->
        currentCoroutineContext().ensureActive()
        database.transaction {
          val playerEntity = dto.toPlayerEntity(id = playerId)
          playersQueries.insertPlayer(playerEntity)

          playersQueries.deletePlayerAgentStats(playerId)
          playersQueries.deletePlayerTeamHistory(playerId)

          dto.agents.forEach { agent ->
            val entity = agent.toEntity(playerId)
            playersQueries.insertPlayerAgentStat(
              player_id = entity.player_id,
              agent_name = entity.agent_name,
              agent_image_url = entity.agent_image_url,
              usage_count = entity.usage_count,
              usage_percent = entity.usage_percent,
              rounds_played = entity.rounds_played,
              rating = entity.rating,
              acs = entity.acs,
              kd_ratio = entity.kd_ratio,
              adr = entity.adr,
              kast = entity.kast,
              kpr = entity.kpr,
              apr = entity.apr,
              fkpr = entity.fkpr,
              fdpr = entity.fdpr,
              kills = entity.kills,
              deaths = entity.deaths,
              assists = entity.assists,
              first_kills = entity.first_kills,
              first_deaths = entity.first_deaths,
            )
          }

          dto.currentTeam
            ?.toEntity(playerId, true)
            ?.let { entity ->
              playersQueries.insertPlayerTeamHistory(
                player_id = entity.player_id,
                team_id = entity.team_id,
                team_name = entity.team_name,
                team_logo_url = entity.team_logo_url,
                is_current = entity.is_current,
              )
            }

          dto.pastTeams
            .map { it.toEntity(playerId, false) }
            .forEach { entity ->
              playersQueries.insertPlayerTeamHistory(
                player_id = entity.player_id,
                team_id = entity.team_id,
                team_name = entity.team_name,
                team_logo_url = entity.team_logo_url,
                is_current = entity.is_current,
              )
            }
        }
        Unit
      }
    }
  }

  private data class PlayerDetailSlices(
    val player: GetPlayerWithFavoriteStatus?,
    val agentStats: List<PlayerAgentStats>,
    val teamHistory: List<PlayerTeamHistory> = emptyList(),
  )
}
