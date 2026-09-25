/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get

internal class FavoriteMatchesDataSourceImpl(private val client: HttpClient) : FavoriteMatchesDataSource {
  override suspend fun list(clientId: String, includeResults: Boolean): Result<List<MatchPreviewDto>> = runCatching {
    client.get("/api/v1/favorites/$clientId/matches") {
      url { parameters.append("include_results", includeResults.toString()) }
    }.body<List<MatchPreviewDto>>()
  }.recoverCatching { error ->
    if (error is ResponseException && error.response.status.value in setOf(404, 503)) {
      throw FavoriteMatchesUnavailableException(error.response.status.value)
    }
    throw error
  }
}
