/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.repository

import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import kotlinx.coroutines.flow.StateFlow

interface FavoriteMatchesRepository {
  val homeMatches: StateFlow<FavoriteMatchFeed?>

  suspend fun fetch(includeResults: Boolean): Result<List<MatchPreview>>
}

data class FavoriteMatchFeed(val selection: DirectFavoriteSnapshot, val matches: List<MatchPreview>)

class FavoriteMatchesRetryLaterException(val statusCode: Int) : Exception("Favorite matches unavailable ($statusCode)")
