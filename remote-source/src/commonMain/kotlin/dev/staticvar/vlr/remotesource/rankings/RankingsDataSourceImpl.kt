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

internal class RankingsDataSourceImpl(private val client: HttpClient) : RankingsDataSource {
  override suspend fun list(): Result<List<TeamRankingDto>> = runCatching {
    // The server re-ranks on every request, so a ranking that moved between pages can repeat or
    // skip a team at a page boundary. Such a read is discarded and fetched again from the start.
    repeat(MAX_ATTEMPTS) { readAllPages()?.let { return@runCatching it } }
    throw IllegalStateException("Rankings changed while they were being read")
  }

  /** Reads every page, or returns null when the ranking changed between pages. */
  private suspend fun readAllPages(): List<TeamRankingDto>? {
    val first = page(offset = 0)
    val total = first.total
    check(total in 0..MAX_PAGES * PAGE_SIZE) { "Unexpected ranking size $total" }
    val teams = first.teams.toMutableList()
    while (teams.size < total) {
      val next = page(offset = teams.size)
      if (next.total != total || next.teams.isEmpty()) return null
      teams += next.teams
    }
    val ids = teams.map { it.team.id }
    check(ids.none(String::isBlank)) { "Ranking contains a team without an ID" }
    if (teams.size != total || ids.toSet().size != ids.size) return null
    return teams
  }

  private suspend fun page(offset: Int): RankingListDto = client.get(ApiPaths.RANKINGS) {
    parameter("circuit", "all")
    parameter("region", "all")
    parameter("min_matches", MIN_MATCHES)
    parameter("include_inactive", false)
    parameter("sort", "elo")
    parameter("order", "desc")
    parameter("limit", PAGE_SIZE)
    parameter("offset", offset)
  }.body()

  private companion object {
    const val MIN_MATCHES = 5
    const val PAGE_SIZE = 200 // the API's maximum
    const val MAX_PAGES = 10
    const val MAX_ATTEMPTS = 3
  }
}
