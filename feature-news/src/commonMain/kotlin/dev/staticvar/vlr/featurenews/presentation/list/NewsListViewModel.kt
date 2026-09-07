/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsListUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class NewsListViewModel(
  observeNewsListUseCase: ObserveNewsListUseCase,
  refreshNewsUseCase: RefreshNewsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshNewsUseCase()
  }

  public val uiState: StateFlow<NewsListUiState> =
    combine(observeNewsListUseCase(), refresher.state) { items, refresh ->
      NewsListUiState(
        items = items,
        isLoading = refresh.isLoading(hasContent = items.isNotEmpty()),
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
        errorDetails = refresh.errorDetails,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NewsListUiState())

  public fun refresh() {
    refresher.refresh()
  }
}
