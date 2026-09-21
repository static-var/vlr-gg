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
import dev.staticvar.vlr.domain.repository.TeamSearchRepository
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

public class RankingsViewModel(
  observeRankingsUseCase: ObserveRankingsUseCase,
  refreshRankingsUseCase: RefreshRankingsUseCase,
  networkMonitor: NetworkMonitor,
  private val teamSearchRepository: TeamSearchRepository,
) : ViewModel() {
  public val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status

  private val selectedRegion: MutableStateFlow<String?> = MutableStateFlow(null)
  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshRankingsUseCase()
  }

  public val uiState: StateFlow<RankingsUiState> =
    combine(observeRankingsUseCase(), selectedRegion, refresher.state) { rankings, region, refresh ->
      RankingsUiState(
        regions = rankings,
        selectedRegion = region?.takeIf { selected -> rankings.any { it.region == selected } }
          ?: rankings.firstOrNull()?.region,
        isLoading = refresh.isLoading(hasContent = rankings.isNotEmpty()),
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
        errorDetails = refresh.errorDetails,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RankingsUiState())

  private val mutableSearchState: MutableStateFlow<TeamSearchUiState> = MutableStateFlow(TeamSearchUiState())
  public val searchState: StateFlow<TeamSearchUiState> = mutableSearchState.asStateFlow()
  private var searchJob: Job? = null

  public fun openSearch() {
    mutableSearchState.value = mutableSearchState.value.copy(isOpen = true)
  }

  public fun closeSearch() {
    searchJob?.cancel()
    mutableSearchState.value = TeamSearchUiState()
  }

  public fun updateSearchQuery(query: String) {
    if (!mutableSearchState.value.isOpen || query == mutableSearchState.value.query) return
    searchJob?.cancel()
    mutableSearchState.value = mutableSearchState.value.copy(query = query, results = TeamSearchResults.Idle)
    search(query.trim(), debounce = true)
  }

  public fun retrySearch() {
    if (!mutableSearchState.value.isOpen || mutableSearchState.value.results !is TeamSearchResults.Error) return
    search(mutableSearchState.value.query.trim(), debounce = false)
  }

  private fun search(query: String, debounce: Boolean) {
    if (query.length < 3) return
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
      if (debounce) delay(300)
      mutableSearchState.value = mutableSearchState.value.copy(results = TeamSearchResults.Loading)
      val result = try {
        teamSearchRepository.searchTeams(query)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (error: Exception) {
        Result.failure(error)
      }
      ensureActive()
      mutableSearchState.value = mutableSearchState.value.copy(
        results = result.fold(
          onSuccess = { TeamSearchResults.Success(it) },
          onFailure = { TeamSearchResults.Error(it.message.orEmpty()) },
        ),
      )
    }
  }

  public fun selectRegion(region: String) {
    selectedRegion.value = region
  }

  public fun refresh() {
    refresher.refresh()
  }
}
