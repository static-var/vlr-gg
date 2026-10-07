/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.domain.model.TeamRanking

public enum class RankingsView { Explore, Regional }

public data class RankingsUiState(
  val teams: List<TeamRanking> = emptyList(),
  val view: RankingsView = RankingsView.Explore,
  val exploreQuery: RankingsQuery = RankingsQuery(),
  val selectedRegion: RankingRegion? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
) {
  public val query: RankingsQuery
    get() = when (view) {
      RankingsView.Explore -> exploreQuery
      RankingsView.Regional -> RankingsQuery(region = selectedRegion)
    }
}
