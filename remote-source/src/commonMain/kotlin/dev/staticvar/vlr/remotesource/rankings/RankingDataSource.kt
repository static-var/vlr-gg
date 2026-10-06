/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.rankings

interface RankingsDataSource {
  /** Every team in the global Elo ranking, in ranked order. */
  suspend fun list(): Result<List<TeamRankingDto>>
}
