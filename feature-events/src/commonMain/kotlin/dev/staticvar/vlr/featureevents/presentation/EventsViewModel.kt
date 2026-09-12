/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.featureevents.usecase.ObserveEventListUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

public class EventsViewModel(
  observeEventListUseCase: ObserveEventListUseCase,
  refreshEventsUseCase: RefreshEventsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshEventsUseCase() }
  private val selectedFilter = MutableStateFlow<EventStatusFilter?>(null)
  private val events = observeEventListUseCase().onEach { events ->
    selectedFilter.update { selected ->
      val preferred = selected ?: events.firstOrNull()?.status?.let(::eventStatusToFilter)
        ?: EventStatusFilter.Ongoing
      events.availableStatusOrSelected(preferred)
    }
  }

  public val uiState: StateFlow<EventsUiState> = combine(
    events, selectedFilter, refresher.state,
  ) { events, selected, refresh ->
    val filter = selected ?: EventStatusFilter.Ongoing
    EventsUiState(
      events = events,
      filteredEvents = events.filterByStatus(filter),
      selectedStatus = filter,
      isLoading = refresh.isLoading(hasContent = events.isNotEmpty()),
      isRefreshing = refresh.isRefreshing,
      errorMessage = refresh.errorMessage,
      errorDetails = refresh.errorDetails,
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, EventsUiState())

  public fun selectFilter(filter: EventStatusFilter) {
    selectedFilter.value = filter
  }

  public fun refresh(): Unit = refresher.refresh()
}

private fun eventStatusToFilter(status: EventStatus): EventStatusFilter = when (status) {
  EventStatus.ONGOING -> EventStatusFilter.Ongoing
  EventStatus.UPCOMING -> EventStatusFilter.Upcoming
  EventStatus.COMPLETED -> EventStatusFilter.Completed
  EventStatus.PAUSED -> EventStatusFilter.Paused
  EventStatus.UNKNOWN -> EventStatusFilter.Unknown
}

internal fun List<EventPreview>.filterByStatus(filter: EventStatusFilter): List<EventPreview> =
  filter { event -> eventStatusToFilter(event.status) == filter }

private fun List<EventPreview>.availableStatusOrSelected(selectedStatus: EventStatusFilter): EventStatusFilter =
  EventStatusFilter.entries.firstOrNull { filter -> filter == selectedStatus && filterByStatus(filter).isNotEmpty() }
    ?: EventStatusFilter.entries.firstOrNull { filter -> filterByStatus(filter).isNotEmpty() }
    ?: selectedStatus
