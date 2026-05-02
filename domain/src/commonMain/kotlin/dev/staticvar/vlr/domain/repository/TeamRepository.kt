/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.TeamInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Team operations.
 * Pure domain contract - no implementation details.
 */
interface TeamRepository {
  /**
   * Get all teams with favorite status.
   */
  fun getTeams(): Flow<List<TeamInfo>>

  /**
   * Get detailed team information by ID.
   * Returns Flow for reactive updates when team data changes.
   */
  fun getTeamDetails(teamId: String): Flow<TeamInfo?>

  /**
   * Get teams by region.
   */
  fun getTeamsByRegion(region: String): Flow<List<TeamInfo>>

  /**
   * Mark a team as favorite.
   */
  suspend fun addToFavorites(teamId: String): Result<Unit>

  /**
   * Remove a team from favorites.
   */
  suspend fun removeFromFavorites(teamId: String): Result<Unit>

  /**
   * Refresh team details from remote source.
   */
  suspend fun refreshTeamDetails(teamId: String): Result<Unit>
}
