/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class TeamDetailsViewModel(
  private val observeTeamDetailsUseCase: ObserveTeamDetailsUseCase,
  private val refreshTeamDetailsUseCase: RefreshTeamDetailsUseCase,
  dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<TeamDetailsUiState> = MutableStateFlow(TeamDetailsUiState())
  private var currentTeamId: String? = null
  private var observeTeamJob: Job? = null

  public val uiState: StateFlow<TeamDetailsUiState> = mutableUiState.asStateFlow()

  public fun openTeam(teamId: String) {
    if (currentTeamId == teamId && observeTeamJob?.isActive == true) {
      return
    }

    currentTeamId = teamId
    observeTeamJob?.cancel()
    mutableUiState.value = TeamDetailsUiState(isLoading = true)
    observeTeamJob =
      scope.launch {
        var initialRefreshRequested = false
        observeTeamDetailsUseCase(teamId).collect { team ->
          mutableUiState.update { current ->
            current.copy(
              team = team,
              isLoading = false,
              errorMessage = if (team != null) null else current.errorMessage,
            )
          }
          if (!initialRefreshRequested && team == null) {
            initialRefreshRequested = true
            refreshInternal(teamId = teamId, showRefreshing = false)
          }
        }
      }
  }

  public fun refresh() {
    val teamId: String = currentTeamId ?: return
    scope.launch {
      refreshInternal(teamId = teamId, showRefreshing = true)
    }
  }

  public fun clear() {
    observeTeamJob?.cancel()
    scope.cancel()
  }

  private suspend fun refreshInternal(teamId: String, showRefreshing: Boolean) {
    if (showRefreshing) {
      mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
    }
    val refreshResult: Result<Unit> = refreshTeamDetailsUseCase(teamId)
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}
