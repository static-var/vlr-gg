/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.featureevents.usecase.ObserveEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventDetailsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class EventDetailsViewModel(
  private val observeEventDetailsUseCase: ObserveEventDetailsUseCase,
  private val refreshEventDetailsUseCase: RefreshEventDetailsUseCase,
  dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<EventDetailsUiState> = MutableStateFlow(EventDetailsUiState())
  private var currentEventId: String? = null
  private var observeEventJob: Job? = null

  public val uiState: StateFlow<EventDetailsUiState> = mutableUiState.asStateFlow()

  public fun openEvent(eventId: String) {
    if (currentEventId == eventId && observeEventJob?.isActive == true) {
      return
    }

    currentEventId = eventId
    observeEventJob?.cancel()
    mutableUiState.value = EventDetailsUiState(isLoading = true)
    observeEventJob =
      scope.launch {
        var initialRefreshRequested = false
        observeEventDetailsUseCase(eventId).collect { event ->
          mutableUiState.update { current ->
            current.copy(
              event = event,
              isLoading = false,
              errorMessage = if (event != null) null else current.errorMessage,
            )
          }
          if (!initialRefreshRequested && event == null) {
            initialRefreshRequested = true
            refreshInternal(eventId = eventId, showRefreshing = false)
          }
        }
      }
  }

  public fun refresh() {
    val eventId: String = currentEventId ?: return
    scope.launch {
      refreshInternal(eventId = eventId, showRefreshing = true)
    }
  }

  public fun clear() {
    observeEventJob?.cancel()
    scope.cancel()
  }

  private suspend fun refreshInternal(eventId: String, showRefreshing: Boolean) {
    if (showRefreshing) {
      mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
    }
    val refreshResult: Result<Unit> = refreshEventDetailsUseCase(eventId)
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}
