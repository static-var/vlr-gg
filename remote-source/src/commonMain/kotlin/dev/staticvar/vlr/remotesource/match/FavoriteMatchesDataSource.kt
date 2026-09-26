/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

interface FavoriteMatchesDataSource {
  suspend fun list(clientId: String, includeResults: Boolean): Result<List<MatchPreviewDto>>
}

class FavoriteMatchesUnavailableException(val statusCode: Int) : Exception("Favorite matches unavailable ($statusCode)")
