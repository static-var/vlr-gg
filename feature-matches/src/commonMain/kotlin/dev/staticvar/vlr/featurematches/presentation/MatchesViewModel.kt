/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchListUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Clock

public class MatchesViewModel(
  observeMatchListUseCase: ObserveMatchListUseCase,
  refreshMatchesUseCase: RefreshMatchesUseCase,
  networkMonitor: NetworkMonitor,
  dispatchers: DispatcherProvider,
) : ViewModel() {
  public val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshMatchesUseCase() }
  private val selectedFilter = MutableStateFlow(MatchStatusFilter.Live)
  private val matches = observeMatchListUseCase()
    .map { all -> all to all.groupByStatus(nowEpochMillis = Clock.System.now().toEpochMilliseconds()) }
    .flowOn(dispatchers.default)

  public val uiState: StateFlow<MatchesUiState> = combine(
    matches, selectedFilter, refresher.state,
  ) { (matches, statusLists), selected, refresh ->
    MatchesUiState(
      matches = matches,
      statusLists = statusLists,
      selectedStatus = selected,
      isLoading = refresh.isLoading(hasContent = matches.isNotEmpty()),
      isRefreshing = refresh.isRefreshing,
      errorMessage = refresh.errorMessage,
      errorDetails = refresh.errorDetails,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, MatchesUiState())

  public fun selectFilter(filter: MatchStatusFilter) {
    selectedFilter.value = filter
  }

  public fun refresh(): Unit = refresher.refresh()
}
