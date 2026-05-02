/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.team

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class TeamDataSourceImpl(private val client: HttpClient) : TeamDataSource {
  override suspend fun details(id: String): Result<TeamDetailsDto> = runCatching {
    client.get(ApiPaths.team(id)).body()
  }
}
