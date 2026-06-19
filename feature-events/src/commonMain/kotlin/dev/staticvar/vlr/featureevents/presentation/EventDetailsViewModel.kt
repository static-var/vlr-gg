/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.featureevents.usecase.ObserveEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventDetailsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
  private var refreshEventJob: Job? = null

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
        var missingEventRefreshRequested = false
        var incompleteDetailsRefreshRequested = false
        observeEventDetailsUseCase(eventId).collect { event ->
          mutableUiState.update { current ->
            val isIncompleteCachedEvent = event?.needsDetailRefresh() == true
            current.copy(
              event = event,
              isLoading = event == null || isIncompleteCachedEvent,
              errorMessage = if (event != null) null else current.errorMessage,
            )
          }
          if (!missingEventRefreshRequested && event == null) {
            missingEventRefreshRequested = true
            refreshEventJob = scope.launch {
              refreshInternal(eventId = eventId, showRefreshing = false)
            }
          }
          if (!incompleteDetailsRefreshRequested && event?.needsDetailRefresh() == true) {
            incompleteDetailsRefreshRequested = true
            refreshEventJob = scope.launch {
              refreshInternal(eventId = eventId, showRefreshing = true)
            }
          }
        }
      }
  }

  public fun refresh() {
    val eventId: String = currentEventId ?: return
    refreshEventJob?.cancel()
    refreshEventJob = scope.launch {
      refreshInternal(eventId = eventId, showRefreshing = true)
    }
  }

  public fun clear() {
    observeEventJob?.cancel()
  }

  private suspend fun refreshInternal(eventId: String, showRefreshing: Boolean) {
    if (showRefreshing) {
      mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
    }
    val refreshResult: Result<Unit> = refreshEventDetailsUseCase(eventId)
    mutableUiState.update { current ->
      val keepLoadingForCachedShell = refreshResult.isSuccess && current.event?.needsDetailRefresh() == true
      current.copy(
        isLoading = keepLoadingForCachedShell,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }

  private fun EventDetails.needsDetailRefresh(): Boolean =
    prizes.isEmpty() && teams.isEmpty() && matches.isEmpty() && standings.isEmpty()
}
