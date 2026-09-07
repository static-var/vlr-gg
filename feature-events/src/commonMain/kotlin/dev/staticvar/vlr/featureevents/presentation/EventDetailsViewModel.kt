/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featureevents.usecase.ObserveEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class EventDetailsViewModel(
  eventId: String,
  observeEventDetailsUseCase: ObserveEventDetailsUseCase,
  refreshEventDetailsUseCase: RefreshEventDetailsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshEventDetailsUseCase(eventId) }

  public val uiState: StateFlow<EventDetailsUiState> = combine(
    observeEventDetailsUseCase(eventId),
    refresher.state,
  ) { event, refresh ->
    EventDetailsUiState(
      event = event,
      isLoading = refresh.isLoading(hasContent = event != null),
      isRefreshing = refresh.isRefreshing,
      isDetailLoadPending = !refresh.hasCompleted || refresh.isRefreshing,
      errorMessage = refresh.errorMessage,
      errorDetails = refresh.errorDetails,
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    EventDetailsUiState(),
  )

  public fun refresh(): Unit = refresher.refresh()
}
