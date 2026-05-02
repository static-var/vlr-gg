/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.PlayerInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Player operations.
 * Pure domain contract - no implementation details.
 */
interface PlayerRepository {

  fun getPlayerInTeam(teamId: String): Flow<List<PlayerInfo?>>

  /**
   * Get detailed player information by ID.
   * Returns Flow for reactive updates when player data changes.
   */
  fun getPlayerDetails(playerId: String): Flow<PlayerInfo?>

  /**
   * Mark a player as favorite.
   */
  suspend fun addToFavorites(playerId: String): Result<Unit>

  /**
   * Remove a player from favorites.
   */
  suspend fun removeFromFavorites(playerId: String): Result<Unit>

  /**
   * Refresh player details from remote source.
   */
  suspend fun refreshPlayerDetails(playerId: String): Result<Unit>
}
