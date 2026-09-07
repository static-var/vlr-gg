/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsArticleUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class NewsArticleViewModel(
  articleId: String,
  observeNewsArticleUseCase: ObserveNewsArticleUseCase,
  refreshNewsArticleUseCase: RefreshNewsArticleUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshNewsArticleUseCase(articleId)
  }

  public val uiState: StateFlow<NewsArticleUiState> =
    combine(observeNewsArticleUseCase(articleId), refresher.state) { article, refresh ->
      NewsArticleUiState(
        article = article,
        isLoading = false,
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NewsArticleUiState())

  public fun refresh() {
    refresher.refresh()
  }
}
