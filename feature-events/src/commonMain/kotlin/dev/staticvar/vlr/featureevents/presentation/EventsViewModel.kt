/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.featureevents.usecase.ObserveEventListUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class EventsViewModel(
  observeEventListUseCase: ObserveEventListUseCase,
  refreshEventsUseCase: RefreshEventsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshEventsUseCase() }
  private val selectedFilter = MutableStateFlow(EventStatusFilter.Ongoing)
  private val events = observeEventListUseCase()

  public val uiState: StateFlow<EventsUiState> = combine(
    events, selectedFilter, refresher.state,
  ) { events, selected, refresh ->
    EventsUiState(
      events = events,
      selectedStatus = selected,
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
