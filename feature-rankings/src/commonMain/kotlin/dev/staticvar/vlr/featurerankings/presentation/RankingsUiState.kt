/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.TeamRanking

public data class RankingsUiState(
  val teams: List<TeamRanking> = emptyList(),
  val selectedRegion: RankingRegion? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
) {
  public val visibleTeams: List<TeamRanking> = selectedRegion?.let { region ->
    var previousGlobalRank: Int? = null
    var regionalRank = 0
    teams.filter { it.region == region }.mapIndexed { index, team ->
      if (team.rank != previousGlobalRank) regionalRank = index + 1
      previousGlobalRank = team.rank
      team.copy(rank = regionalRank)
    }
  } ?: teams
}
