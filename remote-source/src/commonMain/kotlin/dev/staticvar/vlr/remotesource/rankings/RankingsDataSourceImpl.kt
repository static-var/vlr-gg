/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

import dev.staticvar.vlr.remotesource.common.ApiPaths
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.CancellationException

internal class RankingsDataSourceImpl(private val client: HttpClient) : RankingsDataSource {
  override suspend fun list(query: RankingsRequest): Result<List<TeamRankingDto>> = try {
    val page = client.get(ApiPaths.RANKINGS) {
      parameter("circuit", query.circuit)
      parameter("region", query.region)
      parameter("min_matches", query.minMatches)
      parameter("include_inactive", query.includeInactive)
      parameter("sort", query.sort)
      parameter("order", query.order)
      parameter("limit", 50)
      parameter("offset", 0)
    }.body<RankingListDto>()
    check(page.total >= 0 && page.teams.size == minOf(page.total, 50)) {
      "Unexpected ranking response size"
    }
    val ids = page.teams.map { it.team.id }
    check(ids.none(String::isBlank) && ids.distinct().size == ids.size) {
      "Ranking contains missing or repeated team IDs"
    }
    Result.success(page.teams)
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (failure: Exception) {
    Result.failure(failure)
  }
}
