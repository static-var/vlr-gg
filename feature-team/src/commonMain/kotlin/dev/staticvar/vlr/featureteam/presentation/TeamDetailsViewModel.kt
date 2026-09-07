/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

public class TeamDetailsViewModel(
  teamId: String,
  observeTeamDetailsUseCase: ObserveTeamDetailsUseCase,
  refreshTeamDetailsUseCase: RefreshTeamDetailsUseCase,
  networkMonitor: NetworkMonitor,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshTeamDetailsUseCase(teamId)
  }

  public val uiState: StateFlow<TeamDetailsUiState> =
    combine(observeTeamDetailsUseCase(teamId), refresher.state) { team, refresh ->
      TeamDetailsUiState(
        team = team,
        isLoading = false,
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TeamDetailsUiState())

  public fun refresh() {
    refresher.refresh()
  }
}
