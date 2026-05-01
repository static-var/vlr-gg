package dev.staticvar.vlr.featurenews.presentation.list

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.NewsItem
import dev.staticvar.vlr.featurenews.usecase.ObserveNewsListUseCase
import dev.staticvar.vlr.featurenews.usecase.RefreshNewsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class NewsListViewModel(
  private val observeNewsListUseCase: ObserveNewsListUseCase,
  private val refreshNewsUseCase: RefreshNewsUseCase,
  private val dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<NewsListUiState> = MutableStateFlow(NewsListUiState())

  public val uiState: StateFlow<NewsListUiState> = mutableUiState.asStateFlow()

  init {
    scope.launch {
      observeNewsListUseCase().collect { newsItems ->
        mutableUiState.update { current ->
          current.copy(
            items = newsItems,
            isLoading = false,
            errorMessage = if (newsItems.isNotEmpty()) null else current.errorMessage,
          )
        }
      }
    }
    scope.launch(dispatchers.io) {
      val initialItems: List<NewsItem> = observeNewsListUseCase().first()
      if (initialItems.isEmpty()) {
        refreshInternal(showRefreshing = false)
      }
    }
  }

  public fun refresh() {
    scope.launch {
      refreshInternal(showRefreshing = true)
    }
  }

  public fun clear() {
    scope.cancel()
  }

  private suspend fun refreshInternal(showRefreshing: Boolean) {
    if (showRefreshing) {
      mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
    }
    val refreshResult: Result<Unit> = refreshNewsUseCase()
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}
