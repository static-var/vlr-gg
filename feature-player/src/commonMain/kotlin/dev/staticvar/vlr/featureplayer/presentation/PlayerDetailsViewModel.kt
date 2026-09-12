/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.refresh.RefreshController
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.featureplayer.usecase.ObservePlayerDetailsUseCase
import dev.staticvar.vlr.featureplayer.usecase.RefreshPlayerDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

public class PlayerDetailsViewModel(
  private val playerId: String,
  observePlayerDetailsUseCase: ObservePlayerDetailsUseCase,
  refreshPlayerDetailsUseCase: RefreshPlayerDetailsUseCase,
  networkMonitor: NetworkMonitor,
  private val playerRepository: PlayerRepository,
) : ViewModel() {
  public val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

  private val refresher: RefreshController = RefreshController(viewModelScope, networkMonitor) {
    refreshPlayerDetailsUseCase(playerId)
  }

  private val favoriteMutation = MutableStateFlow(FavoriteMutation())

  public val uiState: StateFlow<PlayerDetailsUiState> =
    combine(observePlayerDetailsUseCase(playerId), refresher.state, favoriteMutation) { player, refresh, favorite ->
      PlayerDetailsUiState(
        player = player,
        isLoading = refresh.isLoading(hasContent = player != null),
        isRefreshing = refresh.isRefreshing,
        errorMessage = refresh.errorMessage,
        errorDetails = refresh.errorDetails,
        isUpdatingFavorite = favorite.isPending,
        favoriteErrorMessage = favorite.errorMessage,
      )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerDetailsUiState())

  public fun toggleFavorite() {
    val player = uiState.value.player ?: return
    if (favoriteMutation.value.isPending) return

    favoriteMutation.value = FavoriteMutation(isPending = true)
    viewModelScope.launch {
      try {
        val result = if (player.isFavorite) {
          playerRepository.removeFromFavorites(playerId)
        } else {
          playerRepository.addToFavorites(playerId)
        }
        result.getOrThrow()
        uiState.first { it.player?.isFavorite == !player.isFavorite }
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
