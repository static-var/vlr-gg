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
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class MatchDetailsViewModel(
  matchId: String,
  observeMatchDetailsUseCase: ObserveMatchDetailsUseCase,
  refreshMatchDetailsUseCase: RefreshMatchDetailsUseCase,
  networkMonitor: NetworkMonitor,
  private val preferencesRepository: MatchDetailsPreferencesRepository,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher = RefreshController(viewModelScope, networkMonitor) { refreshMatchDetailsUseCase(matchId) }

  public val uiState: StateFlow<MatchDetailsUiState> = combine(
    observeMatchDetailsUseCase(matchId),
    refresher.state,
    preferencesRepository.preferences,
  ) { match, refresh, preferences ->
    MatchDetailsUiState(
      match = match,
      preferences = preferences,
      isLoading = refresh.isLoading(hasContent = match != null),
      isRefreshing = refresh.isRefreshing,
      errorMessage = refresh.errorMessage,
      errorDetails = refresh.errorDetails,
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    MatchDetailsUiState(preferences = preferencesRepository.preferences.value),
  )

  public fun setPreferences(preferences: MatchDetailsPreferences) {
    preferencesRepository.setPreferences(preferences)
  }

  public fun refresh(): Unit = refresher.refresh()
}
