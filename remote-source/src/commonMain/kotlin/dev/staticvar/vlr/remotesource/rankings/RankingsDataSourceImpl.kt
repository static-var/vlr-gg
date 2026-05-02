/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class RankingsDataSourceImpl(private val client: HttpClient) : RankingsDataSource {
  override suspend fun list(): Result<List<RankingDto>> = runCatching {
    client.get(ApiPaths.RANKINGS).body()
  }
}
