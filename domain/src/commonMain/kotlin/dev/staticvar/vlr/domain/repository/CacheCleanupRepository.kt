/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.CacheCleanupStats
import kotlinx.coroutines.flow.Flow

interface CacheCleanupRepository {
  fun observeStats(): Flow<CacheCleanupStats>

  suspend fun cleanupIfDue(nowEpochMillis: Long): Result<Unit>
}
