/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.telemetry.traceRefresh
import dev.staticvar.vlr.data.cache.EmptyRegionLabelStore
import dev.staticvar.vlr.data.cache.RegionLabelStore
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.data.mapper.toRegionalRankings
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class RankingsRepositoryImpl(
  private val rankingsDataSource: RankingsDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
  private val regionLabels: RegionLabelStore = EmptyRegionLabelStore,
) : RankingsRepository {

  private val rankingsQueries = database.rankingsQueries

  override fun getAllRankings(): Flow<List<RegionalRanking>> = rankingsQueries
    .getRankingsWithFavoriteStatus(region = null)
    .asFlow()
    .mapToList(dispatchers.io)
    .combine(regionLabels.version) { rankings, _ -> rankings.toRegionalRankings(regionLabels::label) }

  override fun getRankingsByRegion(region: String): Flow<RegionalRanking?> = rankingsQueries
    .getRankingsWithFavoriteStatus(region)
    .asFlow()
    .mapToList(dispatchers.io)
    .combine(regionLabels.version) { rankings, _ -> rankings.toRegionalRankings(regionLabels::label).firstOrNull() }

  override suspend fun refreshRankings(): Result<Unit> = traceRefresh(dispatchers.io, "refreshRankings") {
    rankingsDataSource.list().mapCatching { payload ->
      val rankingDtos = payload.value
      traceDatabase {
        database.transaction {
          rankingsQueries.deleteAllRankings()

          rankingDtos.forEach { ranking ->
            ranking.teams.forEach { team ->
              val entity = team.toEntity(ranking.region)
              rankingsQueries.insertRankingDetails(
                team_id = entity.team_id,
                region = entity.region,
                team_name = entity.team_name,
                team_logo = entity.team_logo,
                country = entity.country,
                rank = entity.rank,
                points = entity.points,
                last_updated = entity.last_updated,
              )
            }
          }
        }
      }
      regionLabels.put(
        contentLanguage = payload.contentLanguage,
        labels = rankingDtos.associate { it.region to it.regionLabel.orEmpty() },
      )
      Unit
    }
  }
}
