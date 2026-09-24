/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import dev.staticvar.vlr.domain.model.TeamSearchResult

public data class TeamSearchUiState(
  val isOpen: Boolean = false,
  val query: String = "",
  val results: TeamSearchResults = TeamSearchResults.Idle,
)

public sealed interface TeamSearchResults {
  public data object Idle : TeamSearchResults
  public data object Loading : TeamSearchResults
  public data class Success(val teams: List<TeamSearchResult>) : TeamSearchResults
  public data class Error(val message: String) : TeamSearchResults
}
