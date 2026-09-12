/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featurehome.usecase.ObserveHomeFeedUseCase
import dev.staticvar.vlr.featurehome.usecase.RefreshHomeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class HomeViewModel(
  observeHomeFeedUseCase: ObserveHomeFeedUseCase,
  refreshHomeUseCase: RefreshHomeUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(
    scope = viewModelScope,
    networkMonitor = networkMonitor,
    operation = "refresh home",
  ) { refreshHomeUseCase() }

  public val uiState: StateFlow<HomeUiState> = combine(
    observeHomeFeedUseCase(),
    refresher.state,
  ) { feed, refresh ->
    HomeUiState(
      feed = feed,
      hasLoadedFeed = true,
      isLoading = refresh.isLoading(hasContent = feed.hasDirectFavorites),
      isRefreshing = refresh.isRefreshing,
      errorMessage = refresh.errorMessage,
      errorDetails = refresh.errorDetails,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

  public fun refresh(): Unit = refresher.refresh()
}
