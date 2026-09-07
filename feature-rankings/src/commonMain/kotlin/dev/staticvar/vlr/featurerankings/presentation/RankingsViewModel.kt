/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class RankingsViewModel(
  observeRankingsUseCase: ObserveRankingsUseCase,
  refreshRankingsUseCase: RefreshRankingsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val selectedRegion: MutableStateFlow<String?> = MutableStateFlow(null)
  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshRankingsUseCase()
  }

  public val uiState: StateFlow<RankingsUiState> =
    combine(observeRankingsUseCase(), selectedRegion, refresher.state) { rankings, region, refresh ->
      RankingsUiState(
        regions = rankings,
        selectedRegion = region ?: rankings.firstOrNull()?.region,
        isLoading = false,
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RankingsUiState())

  public fun selectRegion(region: String) {
    selectedRegion.value = region
  }

  public fun refresh() {
    refresher.refresh()
  }
}
