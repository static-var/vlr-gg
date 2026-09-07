/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchPreview

public data class MatchesUiState(
  val matches: List<MatchPreview> = emptyList(),
  val filteredMatches: List<MatchPreview> = emptyList(),
  val selectedStatus: MatchStatusFilter = MatchStatusFilter.Live,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
)

public enum class MatchStatusFilter {
  Live,
  Upcoming,
  Completed,
}
