/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.model.TeamRankingProfile
import org.jetbrains.compose.resources.StringResource

public data class TeamDetailsUiState(
  val team: TeamInfo? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
  val errorDetails: String? = null,
  val isUpdatingFavorite: Boolean = false,
  val favoriteErrorMessage: StringResource? = null,
  val rating: TeamRatingState = TeamRatingState.Loading,
)

public sealed interface TeamRatingState {
  public data object Loading : TeamRatingState

  public data class Available(val profile: TeamRankingProfile) : TeamRatingState

  public data object Unavailable : TeamRatingState
}

public enum class TeamMatchesSection {
  Upcoming,
  Completed,
}
