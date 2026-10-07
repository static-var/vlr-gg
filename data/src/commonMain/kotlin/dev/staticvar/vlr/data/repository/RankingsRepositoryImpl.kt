/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.telemetry.traceRefresh
import dev.staticvar.vlr.data.mapper.toRequest
import dev.staticvar.vlr.data.mapper.toTeamRanking
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.time.Clock

internal class RankingsRepositoryImpl(
  private val rankingsDataSource: RankingsDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : RankingsRepository {

  private val rankingsQueries = database.rankingsQueries

  override fun getRankings(query: RankingsQuery): Flow<List<TeamRanking>> = rankingsQueries
    .getRankingQueryTeamsWithFavoriteStatus(query.cacheKey)
    .asFlow()
    .mapToList(dispatchers.io)
    .map { rankings -> rankings.map { it.toTeamRanking() } }

  override suspend fun refreshRankings(query: RankingsQuery): Result<Unit> = traceRefresh(dispatchers.io, "refreshRankings") {
    rankingsDataSource.list(query.toRequest()).mapCatching { teams ->
      currentCoroutineContext().ensureActive()
      val lastUpdated = Clock.System.now().toEpochMilliseconds()
      traceDatabase {
        database.transaction {
          rankingsQueries.deleteRankingQueryTeams(query.cacheKey)
          teams.forEachIndexed { position, team ->
            rankingsQueries.insertRankingQueryTeam(
              query_key = query.cacheKey,
              team_id = team.team.id,
              team_name = team.team.name,
              team_logo = team.team.logo.orEmpty(),
              country = team.team.country.orEmpty(),
              rank = team.rank.toLong(),
              overall_rank = team.overallRank?.toLong(),
              position = position.toLong(),
              elo = team.elo,
              map_elo = team.mapElo,
              matches_played = team.matches.played.toLong(),
              match_wins = team.matches.wins.toLong(),
              match_losses = team.matches.losses.toLong(),
              win_rate = team.matches.winRate,
              region = RankingRegion.entries.firstOrNull { it.apiValue == team.region }?.apiValue,
              last_updated = lastUpdated,
            )
          }
        }
      }
    }
  }
}
