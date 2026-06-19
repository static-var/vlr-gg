/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchListUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class MatchesViewModel(
  private val observeMatchListUseCase: ObserveMatchListUseCase,
  private val refreshMatchesUseCase: RefreshMatchesUseCase,
  dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<MatchesUiState> = MutableStateFlow(MatchesUiState())

  public val uiState: StateFlow<MatchesUiState> = mutableUiState.asStateFlow()

  init {
    scope.launch {
      observeMatchListUseCase().collect { matches ->
        mutableUiState.update { current ->
          current.withMatches(
            matches = matches,
            isLoading = false,
            errorMessage = if (matches.isNotEmpty()) null else current.errorMessage,
          )
        }
      }
    }
    scope.launch(dispatchers.io) {
      val initialMatches: List<MatchPreview> = observeMatchListUseCase().first()
      if (initialMatches.isEmpty()) {
        refreshInternal(showRefreshing = false)
      } else {
        selectFilter(matchStatusToFilter(initialMatches.first().status))
      }
    }
  }

  public fun selectFilter(filter: MatchStatusFilter) {
    mutableUiState.update { it.withSelectedStatus(filter) }
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
    val refreshResult: Result<Unit> = refreshMatchesUseCase()
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}

private fun matchStatusToFilter(status: MatchStatus): MatchStatusFilter = when (status) {
  MatchStatus.LIVE -> MatchStatusFilter.Live
  MatchStatus.UPCOMING -> MatchStatusFilter.Upcoming
  MatchStatus.COMPLETED -> MatchStatusFilter.Completed
  MatchStatus.UNKNOWN -> MatchStatusFilter.Live
}

private fun MatchesUiState.withMatches(
  matches: List<MatchPreview>,
  isLoading: Boolean,
  errorMessage: String?,
): MatchesUiState {
  val availableStatus = matches.availableStatusOrSelected(selectedStatus)
  return copy(
    matches = matches,
    selectedStatus = availableStatus,
    filteredMatches = matches.filterByStatus(availableStatus),
    isLoading = isLoading,
    errorMessage = errorMessage,
  )
}

private fun MatchesUiState.withSelectedStatus(filter: MatchStatusFilter): MatchesUiState = copy(
  selectedStatus = filter,
  filteredMatches = matches.filterByStatus(filter),
)

private fun List<MatchPreview>.filterByStatus(filter: MatchStatusFilter): List<MatchPreview> = filter { match ->
  when (filter) {
    MatchStatusFilter.Live -> match.status == MatchStatus.LIVE
    MatchStatusFilter.Upcoming -> match.status == MatchStatus.UPCOMING
    MatchStatusFilter.Completed -> match.status == MatchStatus.COMPLETED
  }
}

private fun List<MatchPreview>.availableStatusOrSelected(selectedStatus: MatchStatusFilter): MatchStatusFilter =
  MatchStatusFilter.entries.firstOrNull { filter -> filter == selectedStatus && filterByStatus(filter).isNotEmpty() }
    ?: MatchStatusFilter.entries.firstOrNull { filter -> filterByStatus(filter).isNotEmpty() }
    ?: selectedStatus
