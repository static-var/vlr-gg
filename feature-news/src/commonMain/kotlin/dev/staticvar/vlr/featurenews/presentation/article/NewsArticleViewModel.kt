/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurenews.presentation.article

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsArticleUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsArticleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class NewsArticleViewModel(
  private val observeNewsArticleUseCase: ObserveNewsArticleUseCase,
  private val refreshNewsArticleUseCase: RefreshNewsArticleUseCase,
  private val dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<NewsArticleUiState> = MutableStateFlow(NewsArticleUiState())
  private var currentArticleId: String? = null
  private var observeArticleJob: Job? = null

  public val uiState: StateFlow<NewsArticleUiState> = mutableUiState.asStateFlow()

  public fun openArticle(articleId: String) {
    if (currentArticleId == articleId && observeArticleJob?.isActive == true) {
      return
    }

    currentArticleId = articleId
    observeArticleJob?.cancel()
    mutableUiState.value = NewsArticleUiState(isLoading = true)

    observeArticleJob =
      scope.launch {
        var initialRefreshRequested: Boolean = false
        observeNewsArticleUseCase(articleId).collect { article ->
          mutableUiState.update { current ->
            current.copy(
              article = article,
              isLoading = false,
              errorMessage = if (article != null) null else current.errorMessage,
            )
          }

          if (!initialRefreshRequested && (article == null || article.contentHtml.isBlank())) {
            initialRefreshRequested = true
            refreshInternal(articleId = articleId, showRefreshing = article != null)
          }
        }
      }
  }

  public fun refresh() {
    val articleId: String = currentArticleId ?: return
    scope.launch {
      refreshInternal(articleId = articleId, showRefreshing = true)
    }
  }

  public fun clear() {
    observeArticleJob?.cancel()
    scope.cancel()
  }

  private suspend fun refreshInternal(articleId: String, showRefreshing: Boolean) {
    if (showRefreshing) {
      mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
    }
    val refreshResult: Result<Unit> = refreshNewsArticleUseCase(articleId)
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}
