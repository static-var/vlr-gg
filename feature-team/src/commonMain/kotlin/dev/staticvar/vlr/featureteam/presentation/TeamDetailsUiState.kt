package dev.staticvar.vlr.featureteam.presentation

import dev.staticvar.vlr.domain.model.TeamInfo

public data class TeamDetailsUiState(
  val team: TeamInfo? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)

public enum class TeamMatchesSection {
  Upcoming,
  Completed,
}
