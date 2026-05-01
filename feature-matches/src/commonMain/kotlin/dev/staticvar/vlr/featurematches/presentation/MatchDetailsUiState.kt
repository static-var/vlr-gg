package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.domain.model.MatchDetails

public data class MatchDetailsUiState(
  val match: MatchDetails? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)
