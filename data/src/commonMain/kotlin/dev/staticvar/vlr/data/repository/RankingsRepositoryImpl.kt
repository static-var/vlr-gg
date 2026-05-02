package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.mapper.toRegionalRankings
import dev.staticvar.vlr.data.mapper.toEntity
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class RankingsRepositoryImpl(
  private val rankingsDataSource: RankingsDataSource,
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider
) : RankingsRepository {

  private val rankingsQueries = database.rankingsQueries

  override fun getAllRankings(): Flow<List<RegionalRanking>> =
    rankingsQueries
      .getAllRankings()
      .asFlow()
      .mapToList(dispatchers.io)
      .map { rankings -> rankings.toRegionalRankings() }

  override fun getRankingsByRegion(region: String): Flow<RegionalRanking?> =
    rankingsQueries
      .getRankingsByRegion(region)
      .asFlow()
      .mapToList(dispatchers.io)
      .map { rankings -> rankings.toRegionalRankings().firstOrNull() }

  override suspend fun refreshRankings(): Result<Unit> =
    withContext(dispatchers.io) {
      rankingsDataSource.list().mapCatching { rankingDtos ->
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
                last_updated = entity.last_updated
              )
            }
          }
        }
        Unit
      }
    }
}
