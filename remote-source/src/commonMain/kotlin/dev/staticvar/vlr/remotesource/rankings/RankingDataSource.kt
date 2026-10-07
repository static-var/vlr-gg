/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

interface RankingsDataSource {
  suspend fun list(query: RankingsRequest = RankingsRequest()): Result<List<TeamRankingDto>>
}

data class RankingsRequest(
  val circuit: String = "all",
  val region: String = "all",
  val minMatches: Int = 5,
  val includeInactive: Boolean = false,
  val sort: String = "elo",
  val order: String = "desc",
)
