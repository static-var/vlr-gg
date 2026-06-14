/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.featureevents.usecase.ObserveEventListUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class EventsViewModel(
  private val observeEventListUseCase: ObserveEventListUseCase,
  private val refreshEventsUseCase: RefreshEventsUseCase,
  dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<EventsUiState> = MutableStateFlow(EventsUiState())

  public val uiState: StateFlow<EventsUiState> = mutableUiState.asStateFlow()

  init {
    scope.launch {
      observeEventListUseCase().collect { events ->
        mutableUiState.update { current ->
          current.withEvents(
            events = events,
            isLoading = false,
            errorMessage = if (events.isNotEmpty()) null else current.errorMessage,
          )
        }
      }
    }
    scope.launch(dispatchers.io) {
      val initialEvents: List<EventPreview> = observeEventListUseCase().first()
      if (initialEvents.isEmpty()) {
        refreshInternal(showRefreshing = false)
      } else {
        selectFilter(eventStatusToFilter(initialEvents.first().status))
      }
    }
  }

  public fun selectFilter(filter: EventStatusFilter) {
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
    val refreshResult: Result<Unit> = refreshEventsUseCase()
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}

private fun eventStatusToFilter(status: EventStatus): EventStatusFilter = when (status) {
  EventStatus.ONGOING -> EventStatusFilter.Ongoing
  EventStatus.UPCOMING -> EventStatusFilter.Upcoming
  EventStatus.COMPLETED -> EventStatusFilter.Completed
  EventStatus.UNKNOWN -> EventStatusFilter.Ongoing
}

private fun EventsUiState.withEvents(
  events: List<EventPreview>,
  isLoading: Boolean,
  errorMessage: String?,
): EventsUiState = copy(
  events = events,
  filteredEvents = events.filterByStatus(selectedStatus),
  isLoading = isLoading,
  errorMessage = errorMessage,
)

private fun EventsUiState.withSelectedStatus(filter: EventStatusFilter): EventsUiState = copy(
  selectedStatus = filter,
  filteredEvents = events.filterByStatus(filter),
)

private fun List<EventPreview>.filterByStatus(filter: EventStatusFilter): List<EventPreview> = filter { event ->
  when (filter) {
    EventStatusFilter.Ongoing -> event.status == EventStatus.ONGOING
    EventStatusFilter.Upcoming -> event.status == EventStatus.UPCOMING
    EventStatusFilter.Completed -> event.status == EventStatus.COMPLETED
  }
}
