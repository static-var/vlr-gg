package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.RegionalRanking
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Rankings operations.
 * Pure domain contract - no implementation details.
 */
interface RankingsRepository {
  /**
   * Get all regional rankings.
   */
  fun getAllRankings(): Flow<List<RegionalRanking>>

  /**
   * Get rankings for a specific region.
   */
  fun getRankingsByRegion(region: String): Flow<RegionalRanking?>

  /**
   * Refresh rankings from remote source.
   */
  suspend fun refreshRankings(): Result<Unit>
}
