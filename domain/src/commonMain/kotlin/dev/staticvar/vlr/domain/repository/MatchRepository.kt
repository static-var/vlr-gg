package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Match operations.
 * Pure domain contract - no implementation details.
 */
interface MatchRepository {
  /**
   * Get all match previews (overview list).
   */
  fun getMatches(): Flow<List<MatchPreview>>

  /**
   * Get detailed match information by ID.
   * Returns Flow for reactive updates when match data changes.
   */
  fun getMatchDetails(matchId: String): Flow<MatchDetails?>

  /**
   * Mark a match as favorite.
   */
  suspend fun addToFavorites(matchId: String): Result<Unit>

  /**
   * Remove a match from favorites.
   */
  suspend fun removeFromFavorites(matchId: String): Result<Unit>

  /**
   * Refresh matches from remote source.
   */
  suspend fun refreshMatches(): Result<Unit>

  /**
   * Refresh match detail from remote source.
   */
  suspend fun refreshMatchDetails(matchId: String): Result<Unit>
}
