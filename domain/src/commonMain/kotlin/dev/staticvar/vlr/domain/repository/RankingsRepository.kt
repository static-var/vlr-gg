/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.model.RankingsQuery
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Rankings operations.
 * Pure domain contract - no implementation details.
 */
interface RankingsRepository {
  /**
   * Observe the first 50 teams for this selection, in server order.
   */
  fun getRankings(query: RankingsQuery = RankingsQuery()): Flow<List<TeamRanking>>

  /**
   * Refresh rankings from remote source.
   */
  suspend fun refreshRankings(query: RankingsQuery = RankingsQuery()): Result<Unit>
}
