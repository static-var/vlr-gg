/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.EventRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
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
  favoritesRepository: FavoritesRepository,
  private val eventRepository: EventRepository,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshEventDetailsUseCase(eventId) }

  private val favoriteSaving = MutableStateFlow(false)
  private val favoriteError = MutableStateFlow<String?>(null)

  public val uiState: StateFlow<EventDetailsUiState> = combine(
    observeEventDetailsUseCase(eventId),
    refresher.state,
    favoritesRepository.observeTeamIds(),
    favoriteSaving,
    favoriteError,
  ) { event, refresh, favoriteTeamIds, isSavingFavorite, favoriteErrorMessage ->
    EventDetailsUiState(
      event = event,
      isSavingFavorite = isSavingFavorite,
      favoriteErrorMessage = favoriteErrorMessage,
      favoriteTeamIds = favoriteTeamIds,
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

  public fun toggleFavorite() {
    val event = uiState.value.event ?: return
    if (favoriteSaving.value) return
    favoriteSaving.value = true
    favoriteError.value = null
    viewModelScope.launch {
      try {
        val result = if (event.isFavorite) eventRepository.removeFromFavorites(event.id)
          else eventRepository.addToFavorites(event.id)
        result.getOrThrow()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (error: Exception) {
        favoriteError.value = "Couldn't update this event's favorite. Try again."
      } finally {
        favoriteSaving.value = false
      }
    }
  }

  public fun refresh(): Unit = refresher.refresh()
}
