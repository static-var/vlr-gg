package dev.staticvar.vlr.featurerankings.presentation

import dev.staticvar.vlr.domain.model.RegionalRanking

public data class RankingsUiState(
  val regions: List<RegionalRanking> = emptyList(),
  val selectedRegion: String? = null,
  val isLoading: Boolean = true,
  val isRefreshing: Boolean = false,
  val errorMessage: String? = null,
)
