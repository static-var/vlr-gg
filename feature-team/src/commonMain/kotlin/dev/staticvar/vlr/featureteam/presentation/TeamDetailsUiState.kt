/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import dev.staticvar.vlr.domain.model.TeamInfo

public data class TeamDetailsUiState(
  val team: TeamInfo? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
)

public enum class TeamMatchesSection {
  Upcoming,
  Completed,
}
