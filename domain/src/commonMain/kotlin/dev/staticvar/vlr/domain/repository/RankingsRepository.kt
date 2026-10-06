/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.TeamRanking
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Rankings operations.
 * Pure domain contract - no implementation details.
 */
interface RankingsRepository {
  /**
   * Get the global Elo ranking, best team first.
   */
  fun getRankings(): Flow<List<TeamRanking>>

  /**
   * Refresh rankings from remote source.
   */
  suspend fun refreshRankings(): Result<Unit>
}
