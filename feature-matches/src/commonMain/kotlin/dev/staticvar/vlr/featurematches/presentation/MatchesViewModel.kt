/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchListUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Instant

public class MatchesViewModel(
  observeMatchListUseCase: ObserveMatchListUseCase,
  refreshMatchesUseCase: RefreshMatchesUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshMatchesUseCase() }
  private val selectedFilter = MutableStateFlow<MatchStatusFilter?>(null)
  private val matches = observeMatchListUseCase().onEach { matches ->
    selectedFilter.update { selected ->
      val preferred = selected ?: matches.firstOrNull()?.status?.let(::matchStatusToFilter)
        ?: MatchStatusFilter.Live
      matches.availableStatusOrSelected(preferred)
    }
  }

  public val uiState: StateFlow<MatchesUiState> = combine(
    matches, selectedFilter, refresher.state,
  ) { matches, selected, refresh ->
    val filter = selected ?: MatchStatusFilter.Live
    MatchesUiState(
      matches = matches,
      filteredMatches = matches.filterByStatus(filter),
      selectedStatus = filter,
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

private fun matchStatusToFilter(status: MatchStatus): MatchStatusFilter = when (status) {
  MatchStatus.LIVE -> MatchStatusFilter.Live
  MatchStatus.UPCOMING -> MatchStatusFilter.Upcoming
  MatchStatus.COMPLETED -> MatchStatusFilter.Completed
  MatchStatus.UNKNOWN -> MatchStatusFilter.Live
}

internal fun List<MatchPreview>.filterByStatus(filter: MatchStatusFilter): List<MatchPreview> {
  val filtered = filter { match ->
    when (filter) {
      MatchStatusFilter.Live -> match.status == MatchStatus.LIVE
      MatchStatusFilter.Upcoming -> match.status == MatchStatus.UPCOMING
      MatchStatusFilter.Completed -> match.status == MatchStatus.COMPLETED
    }
  }
  return when (filter) {
    MatchStatusFilter.Live -> filtered.sortedBy { match -> match.sortEpochMillis(nullsLast = true) }
    MatchStatusFilter.Upcoming -> filtered.sortedWith(upcomingMatchComparator(nowEpochMillis = Clock.System.now().toEpochMilliseconds()))
    MatchStatusFilter.Completed -> filtered.sortedByDescending { match -> match.sortEpochMillis(nullsLast = false) }
  }
}

private fun upcomingMatchComparator(nowEpochMillis: Long): Comparator<MatchPreview> = compareBy<MatchPreview> { match ->
  match.sortEpochMillis(nullsLast = true) < nowEpochMillis
}.thenBy { match ->
  match.sortEpochMillis(nullsLast = true)
}

private fun MatchPreview.sortEpochMillis(nullsLast: Boolean): Long = time
  ?.takeIf(String::isNotBlank)
  ?.let { value -> runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull() }
  ?: if (nullsLast) Long.MAX_VALUE else Long.MIN_VALUE

private fun List<MatchPreview>.availableStatusOrSelected(selectedStatus: MatchStatusFilter): MatchStatusFilter =
  MatchStatusFilter.entries.firstOrNull { filter -> filter == selectedStatus && filterByStatus(filter).isNotEmpty() }
    ?: MatchStatusFilter.entries.firstOrNull { filter -> filterByStatus(filter).isNotEmpty() }
    ?: selectedStatus
