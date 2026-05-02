/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.match

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

internal class MatchDataSourceImpl(private val client: HttpClient) : MatchDataSource {
  override suspend fun list(): Result<List<MatchPreviewDto>> = runCatching {
    client.get(ApiPaths.MATCHES).body()
  }

  override suspend fun details(id: String): Result<MatchDetailsDto> = runCatching {
    client.get(ApiPaths.match(id)).body<MatchDetailsDto>().normalizeEventName()
  }
}

private fun MatchDetailsDto.normalizeEventName(): MatchDetailsDto {
  val normalizedEvent =
    if (event.name.isNotBlank()) {
      event
    } else {
      event.copy(name = event.series.ifBlank { event.id })
    }
  return if (normalizedEvent == event) this else copy(event = normalizedEvent)
}
