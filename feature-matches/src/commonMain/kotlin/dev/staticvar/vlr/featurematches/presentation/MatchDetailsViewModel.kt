/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchDetailsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class MatchDetailsViewModel(
  private val observeMatchDetailsUseCase: ObserveMatchDetailsUseCase,
  private val refreshMatchDetailsUseCase: RefreshMatchDetailsUseCase,
  dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<MatchDetailsUiState> = MutableStateFlow(MatchDetailsUiState())
  private var currentMatchId: String? = null
  private var observeMatchJob: Job? = null
  private var refreshMatchJob: Job? = null

  public val uiState: StateFlow<MatchDetailsUiState> = mutableUiState.asStateFlow()

  public fun openMatch(matchId: String) {
    if (currentMatchId == matchId && observeMatchJob?.isActive == true) {
      return
    }

    currentMatchId = matchId
    observeMatchJob?.cancel()
    mutableUiState.value = MatchDetailsUiState(isLoading = true)
    observeMatchJob =
      scope.launch {
        var missingMatchRefreshRequested = false
        var incompleteDetailsRefreshRequested = false
        observeMatchDetailsUseCase(matchId).collect { match ->
          mutableUiState.update { current ->
            val isIncompleteCachedMatch = match?.needsDetailRefresh() == true
            current.copy(
              match = match,
              isLoading = match == null || isIncompleteCachedMatch,
              errorMessage = if (match != null) null else current.errorMessage,
            )
          }
          if (!missingMatchRefreshRequested && match == null) {
            missingMatchRefreshRequested = true
            refreshMatchJob = scope.launch {
              refreshInternal(matchId = matchId, showRefreshing = false)
            }
          }
          if (!incompleteDetailsRefreshRequested && match?.needsDetailRefresh() == true) {
            incompleteDetailsRefreshRequested = true
            refreshMatchJob = scope.launch {
              refreshInternal(matchId = matchId, showRefreshing = true)
            }
          }
        }
      }
  }

  public fun refresh() {
    val matchId: String = currentMatchId ?: return
    refreshMatchJob?.cancel()
    refreshMatchJob = scope.launch {
      refreshInternal(matchId = matchId, showRefreshing = true)
    }
  }

  public fun clear() {
    observeMatchJob?.cancel()
  }

  private suspend fun refreshInternal(matchId: String, showRefreshing: Boolean) {
    if (showRefreshing) {
      mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
    }
    val refreshResult: Result<Unit> = refreshMatchDetailsUseCase(matchId)
    mutableUiState.update { current ->
      val keepLoadingForCachedShell = refreshResult.isSuccess && current.match?.needsDetailRefresh() == true
      current.copy(
        isLoading = keepLoadingForCachedShell,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}

private fun MatchDetails.needsDetailRefresh(): Boolean =
  matchData.isEmpty() && head2head.isEmpty() && videos.streams.isEmpty() && videos.vods.isEmpty()
