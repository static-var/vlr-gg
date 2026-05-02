/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.standings

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class StandingsDataSourceImpl(private val client: HttpClient) : StandingsDataSource {
  override suspend fun byYear(year: Int): Result<StandingsDto> = runCatching {
    client.get(ApiPaths.standings(year)).body()
  }
}
