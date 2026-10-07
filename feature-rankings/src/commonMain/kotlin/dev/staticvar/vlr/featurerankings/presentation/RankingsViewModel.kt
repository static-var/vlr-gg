/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class RankingsViewModel(
  private val observeRankingsUseCase: ObserveRankingsUseCase,
  private val refreshRankingsUseCase: RefreshRankingsUseCase,
  private val networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status

  private val mutableUiState = MutableStateFlow(RankingsUiState())
  public val uiState: StateFlow<RankingsUiState> = mutableUiState.asStateFlow()

  private var queryScope: CoroutineScope? = null
  private var refresher: RefreshController? = null
  private var queryGeneration = 0

  init {
    observeQuery(uiState.value.query, refresh = false)
  }

  public fun setView(view: RankingsView) {
    updateSelection { it.copy(view = view) }
  }

  public fun updateExploreQuery(query: RankingsQuery) {
    updateSelection { it.copy(exploreQuery = query) }
  }

  public fun selectRegion(region: RankingRegion?) {
    updateSelection { it.copy(selectedRegion = region) }
  }

  public fun refresh() {
    refresher?.refresh()
  }

  private fun updateSelection(update: (RankingsUiState) -> RankingsUiState) {
    val previousQuery = uiState.value.query
    mutableUiState.update { current ->
      val selected = update(current)
      if (selected.query == current.query) {
        selected
      } else {
        selected.copy(
          teams = emptyList(),
          isLoading = true,
          isRefreshing = false,
          errorMessage = null,
          errorDetails = null,
        )
      }
    }
    val query = uiState.value.query
    if (query != previousQuery) observeQuery(query, refresh = true)
  }

  private fun observeQuery(query: RankingsQuery, refresh: Boolean) {
    queryScope?.cancel()
    val generation = ++queryGeneration
    val scope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
    queryScope = scope
    val controller = RefreshController(scope, networkMonitor, operation = "refresh rankings") {
      refreshRankingsUseCase(query)
    }
    refresher = controller
    scope.launch {
      combine(observeRankingsUseCase(query), controller.state) { teams, refreshState -> teams to refreshState }
        .collect { (teams, refreshState) ->
          if (generation == queryGeneration) {
            mutableUiState.update {
              it.copy(
                teams = teams,
                isLoading = refreshState.isLoading(hasContent = teams.isNotEmpty()),
                isRefreshing = refreshState.isRefreshing,
                errorMessage = refreshState.errorMessage,
                errorDetails = refreshState.errorDetails,
              )
            }
          }
        }
    }
    if (refresh) controller.refresh()
  }
}
