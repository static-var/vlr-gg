/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.CacheCleanupStats
import dev.staticvar.vlr.domain.repository.CacheCleanupRepository
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class CacheCleanupRepositoryImpl(
  private val database: VlrDatabase,
  private val dispatchers: DispatcherProvider,
) : CacheCleanupRepository {

  private val queries = database.cacheCleanupQueries

  override fun observeStats(): Flow<CacheCleanupStats> = queries
    .getCacheCleanupState()
    .asFlow()
    .mapToOneOrNull(dispatchers.io)
    .map { state ->
      CacheCleanupStats(
        deletedRecords = state?.deleted_records ?: 0,
        lastRunEpochMillis = state?.last_run_epoch_millis,
      )
    }

  override suspend fun cleanupIfDue(nowEpochMillis: Long): Result<Unit> = withContext(dispatchers.io) {
    try {
      database.transaction {
        val state = queries.getCacheCleanupState().executeAsOneOrNull()
        val lastRun = state?.last_run_epoch_millis
        val isDue = lastRun == null || lastRun <= nowEpochMillis - CLEANUP_INTERVAL_MILLIS
        if (isDue) {
          val cutoff = nowEpochMillis - CACHE_RETENTION_MILLIS
          val deletedRecords = queries.countCacheCleanupCandidates(cutoff).executeAsOne()

          queries.deleteStaleMatchOverviews(cutoff)
          queries.deleteStaleMatches(cutoff)
          queries.deleteStaleEventOverviews(cutoff)
          queries.deleteStaleEvents(cutoff)
          queries.deleteStaleRankings(cutoff)
          queries.deleteStaleStandings(cutoff)
          queries.deleteStaleTeams(cutoff)
          queries.deleteStalePlayers(cutoff)
          queries.deleteStaleNews(cutoff)
          queries.deleteOrphanedSearchEntries()
          queries.saveCacheCleanupState(
            deletedRecords = (state?.deleted_records ?: 0) + deletedRecords,
            lastRunEpochMillis = nowEpochMillis,
          )
        }
      }
      Result.success(Unit)
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (failure: Throwable) {
      Result.failure(failure)
    }
  }

  private companion object {
    const val CLEANUP_INTERVAL_MILLIS = 24L * 60 * 60 * 1000
    const val CACHE_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
  }
}
