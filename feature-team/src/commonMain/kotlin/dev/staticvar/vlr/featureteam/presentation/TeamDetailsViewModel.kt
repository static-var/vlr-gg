/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.repository.TeamRankingProfileRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import vlr.feature_team.generated.resources.Res
import vlr.feature_team.generated.resources.favorite_update_failed

public class TeamDetailsViewModel(
  private val teamId: String,
  observeTeamDetailsUseCase: ObserveTeamDetailsUseCase,
  refreshTeamDetailsUseCase: RefreshTeamDetailsUseCase,
  networkMonitor: NetworkMonitor,
  private val teamRepository: TeamRepository,
  private val teamRankingProfileRepository: TeamRankingProfileRepository,
) : ViewModel() {
  public val networkStatus: StateFlow<NetworkStatus> = networkMonitor.status

  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshTeamDetailsUseCase(teamId)
  }

  private val favoriteMutation = MutableStateFlow(FavoriteMutation())
  private val ratingRefreshFailed = MutableStateFlow(false)
  private var ratingRefreshJob: Job? = null

  public val uiState: StateFlow<TeamDetailsUiState> =
    combine(observeTeamDetailsUseCase(teamId), refresher.state, favoriteMutation, ratingRefreshFailed) {
        team,
        refresh,
        favorite,
        ratingRefreshFailed,
      ->
      TeamDetailsUiState(
        team = team,
        isLoading = refresh.isLoading(hasContent = team != null),
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
        errorDetails = refresh.errorDetails,
        isUpdatingFavorite = favorite.isPending,
        favoriteErrorMessage = favorite.errorMessage,
        rating = team?.rankingProfile?.let(TeamRatingState::Available)
          ?: if (ratingRefreshFailed) TeamRatingState.Unavailable else TeamRatingState.Loading,
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
        result.getOrThrow()
        uiState.first { it.team?.isFavorite == !team.isFavorite }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        favoriteMutation.value = FavoriteMutation(errorMessage = Res.string.favorite_update_failed)
      } finally {
        if (favoriteMutation.value.isPending) favoriteMutation.value = FavoriteMutation()
      }
    }
  }

  private data class FavoriteMutation(val isPending: Boolean = false, val errorMessage: StringResource? = null)

  public fun refresh() {
    refresher.refresh()
    refreshRating()
  }

  private fun refreshRating() {
    if (ratingRefreshJob?.isActive == true) return
    ratingRefreshFailed.value = false

    ratingRefreshJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
      try {
        teamRankingProfileRepository.refreshProfile(teamId).getOrThrow()
        currentCoroutineContext().ensureActive()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        currentCoroutineContext().ensureActive()
        ratingRefreshFailed.value = true
      }
    }.also { it.start() }
  }
}
