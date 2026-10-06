/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.telemetry.traceRefresh
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.data.mapper.toTeamRanking
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class RankingsRepositoryImpl(
  private val rankingsDataSource: RankingsDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : RankingsRepository {

  private val rankingsQueries = database.rankingsQueries

  override fun getRankings(): Flow<List<TeamRanking>> = rankingsQueries
    .getRankingsWithFavoriteStatus()
    .asFlow()
    .mapToList(dispatchers.io)
    .map { rankings -> rankings.map { it.toTeamRanking() } }

  override suspend fun refreshRankings(): Result<Unit> = traceRefresh(dispatchers.io, "refreshRankings") {
    rankingsDataSource.list().mapCatching { teams ->
      val lastUpdated = Clock.System.now().toEpochMilliseconds()
      traceDatabase {
        database.transaction {
          rankingsQueries.deleteAllRankings()
          teams.forEachIndexed { position, team ->
            val entity = team.toEntity(position, lastUpdated)
            rankingsQueries.insertRanking(
              team_id = entity.team_id,
              team_name = entity.team_name,
              team_logo = entity.team_logo,
              country = entity.country,
              rank = entity.rank,
              position = entity.position,
              elo = entity.elo,
              match_wins = entity.match_wins,
              match_losses = entity.match_losses,
              last_updated = entity.last_updated,
              region = entity.region,
            )
          }
        }
      }
    }
  }
}
