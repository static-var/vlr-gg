package dev.staticvar.vlr.featurerankings.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class RankingsViewModel(
  private val observeRankingsUseCase: ObserveRankingsUseCase,
  private val refreshRankingsUseCase: RefreshRankingsUseCase,
  dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<RankingsUiState> = MutableStateFlow(RankingsUiState())

  public val uiState: StateFlow<RankingsUiState> = mutableUiState.asStateFlow()

  init {
    scope.launch {
      observeRankingsUseCase().collect { rankings ->
        mutableUiState.update { current ->
          current.copy(
            regions = rankings,
            selectedRegion = current.selectedRegion ?: rankings.firstOrNull()?.region,
            isLoading = false,
            errorMessage = if (rankings.isNotEmpty()) null else current.errorMessage,
          )
        }
      }
    }
    scope.launch(dispatchers.io) {
      val initialRankings: List<RegionalRanking> = observeRankingsUseCase().first()
      if (initialRankings.isEmpty()) {
        refreshInternal(showRefreshing = false)
      }
    }
  }

  public fun selectRegion(region: String) {
    mutableUiState.update { it.copy(selectedRegion = region) }
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
    val refreshResult: Result<Unit> = refreshRankingsUseCase()
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}
