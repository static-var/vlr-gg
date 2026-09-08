/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

public class TeamDetailsViewModel(
  private val teamId: String,
  observeTeamDetailsUseCase: ObserveTeamDetailsUseCase,
  refreshTeamDetailsUseCase: RefreshTeamDetailsUseCase,
  networkMonitor: NetworkMonitor,
  private val teamRepository: TeamRepository,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshTeamDetailsUseCase(teamId)
  }

  private val favoriteMutation = MutableStateFlow(FavoriteMutation())

  public val uiState: StateFlow<TeamDetailsUiState> =
    combine(observeTeamDetailsUseCase(teamId), refresher.state, favoriteMutation) { team, refresh, favorite ->
      TeamDetailsUiState(
        team = team,
        isLoading = refresh.isLoading(hasContent = team != null),
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
        errorDetails = refresh.errorDetails,
        isUpdatingFavorite = favorite.isPending,
        favoriteErrorMessage = favorite.errorMessage,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TeamDetailsUiState())

  public fun toggleFavorite() {
    val team = uiState.value.team ?: return
    if (favoriteMutation.value.isPending) return

    favoriteMutation.value = FavoriteMutation(isPending = true)
    viewModelScope.launch {
      try {
        val result = if (team.isFavorite) {
          teamRepository.removeFromFavorites(teamId)
        } else {
          teamRepository.addToFavorites(teamId)
        }
        result.exceptionOrNull()?.let { failure ->
          if (failure is CancellationException) throw failure
        }
        favoriteMutation.value = FavoriteMutation(
          errorMessage = if (result.isFailure) "Couldn't update favorite. Try again." else null,
        )
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        favoriteMutation.value = FavoriteMutation(errorMessage = "Couldn't update favorite. Try again.")
      } finally {
        if (favoriteMutation.value.isPending) favoriteMutation.value = FavoriteMutation()
      }
    }
  }

  private data class FavoriteMutation(val isPending: Boolean = false, val errorMessage: String? = null)

  public fun refresh() {
    refresher.refresh()
  }
}
