/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.core.settings.MatchDetailsPreferences
import dev.staticvar.vlr.core.settings.MatchDetailsPreferencesRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.SetMatchFavoriteUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class MatchDetailsViewModel(
  private val matchId: String,
  observeMatchDetailsUseCase: ObserveMatchDetailsUseCase,
  refreshMatchDetailsUseCase: RefreshMatchDetailsUseCase,
  networkMonitor: NetworkMonitor,
  setMatchFavoriteUseCase: SetMatchFavoriteUseCase,
  favoritesRepository: FavoritesRepository,
  private val preferencesRepository: MatchDetailsPreferencesRepository,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshMatchDetailsUseCase(matchId) }

  private val favorites = MatchFavoriteController(viewModelScope, setMatchFavoriteUseCase)

  private val contentState = combine(
    observeMatchDetailsUseCase(matchId),
    refresher.state,
    preferencesRepository.preferences,
    favoritesRepository.observeTeamIds(),
    favoritesRepository.observePlayerIds(),
  ) { match, refresh, preferences, favoriteTeamIds, favoritePlayerIds ->
    MatchDetailsUiState(
      match = match,
      favoriteTeamIds = favoriteTeamIds,
      favoritePlayerIds = favoritePlayerIds,
      preferences = preferences,
      isLoading = refresh.isLoading(hasContent = match != null),
      isRefreshing = refresh.isRefreshing,
      isDetailLoadPending = !refresh.hasCompleted || refresh.isRefreshing,
      errorMessage = refresh.errorMessage,
      errorDetails = refresh.errorDetails,
    )
  }

  public val uiState: StateFlow<MatchDetailsUiState> = combine(contentState, favorites.state) { content, favorite ->
    content.copy(
      isFavoritePending = matchId in favorite.pendingIds,
      favoriteErrorMessage = favorite.errorMessage,
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    MatchDetailsUiState(preferences = preferencesRepository.preferences.value),
  )

  public fun setPreferences(preferences: MatchDetailsPreferences) {
    preferencesRepository.setPreferences(preferences)
  }

  public fun toggleFavorite() {
    if (!uiState.value.canToggleFavorite) return
    val match = uiState.value.match ?: return
    favorites.toggle(matchId, match.isDirectFavorite) { selected ->
      uiState.first { it.match?.isDirectFavorite == selected }
    }
  }

  public fun refresh(): Unit = refresher.refresh()
}
