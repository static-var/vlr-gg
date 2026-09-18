/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.team

import dev.staticvar.vlr.remotesource.common.ApiPaths
import dev.staticvar.vlr.remotesource.network.RemotePayload
import dev.staticvar.vlr.remotesource.network.bodyAsRemotePayload
import io.ktor.client.HttpClient
import io.ktor.client.request.get

internal class TeamDataSourceImpl(private val client: HttpClient) : TeamDataSource {
  override suspend fun details(id: String): Result<RemotePayload<TeamDetailsDto>> = runCatching {
    client.get(ApiPaths.team(id)).bodyAsRemotePayload()
  }
}
