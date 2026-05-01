package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Event operations.
 * Pure domain contract - no implementation details.
 */
interface EventRepository {
  /**
   * Get all event previews (list view).
   */
  fun getEvents(): Flow<List<EventPreview>>

  /**
   * Get detailed event information by ID.
   * Returns Flow for reactive updates when event data changes.
   */
  fun getEventDetails(eventId: String): Flow<EventDetails?>

  /**
   * Mark an event as favorite.
   */
  suspend fun addToFavorites(eventId: String): Result<Unit>

  /**
   * Remove an event from favorites.
   */
  suspend fun removeFromFavorites(eventId: String): Result<Unit>

  /**
   * Refresh events from remote source.
   */
  suspend fun refreshEvents(): Result<Unit>

  /**
   * Refresh event details from remote source.
   */
  suspend fun refreshEventDetails(eventId: String): Result<Unit>
}
