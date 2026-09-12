/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import kotlinx.coroutines.flow.Flow

interface FavoriteScheduleRepository {
  fun observeMatches(): Flow<List<FavoriteScheduledMatch>>
}
