/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featureplayer.usecase.ObservePlayerDetailsUseCase
import dev.staticvar.vlr.featureplayer.usecase.RefreshPlayerDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class PlayerDetailsViewModel(
  playerId: String,
  observePlayerDetailsUseCase: ObservePlayerDetailsUseCase,
  refreshPlayerDetailsUseCase: RefreshPlayerDetailsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshPlayerDetailsUseCase(playerId)
  }

  public val uiState: StateFlow<PlayerDetailsUiState> =
    combine(observePlayerDetailsUseCase(playerId), refresher.state) { player, refresh ->
      PlayerDetailsUiState(
        player = player,
        isLoading = refresh.isLoading(hasContent = player != null),
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
        errorDetails = refresh.errorDetails,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerDetailsUiState())

  public fun refresh() {
    refresher.refresh()
  }
}
