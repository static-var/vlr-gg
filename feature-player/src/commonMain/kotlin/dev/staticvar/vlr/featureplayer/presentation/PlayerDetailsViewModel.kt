package dev.staticvar.vlr.featureplayer.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.featureplayer.usecase.ObservePlayerDetailsUseCase
import dev.staticvar.vlr.featureplayer.usecase.RefreshPlayerDetailsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

public class PlayerDetailsViewModel(
  private val observePlayerDetailsUseCase: ObservePlayerDetailsUseCase,
  private val refreshPlayerDetailsUseCase: RefreshPlayerDetailsUseCase,
  dispatchers: DispatcherProvider,
) {
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private val mutableUiState: MutableStateFlow<PlayerDetailsUiState> = MutableStateFlow(PlayerDetailsUiState())
  private var currentPlayerId: String? = null
  private var observePlayerJob: Job? = null

  public val uiState: StateFlow<PlayerDetailsUiState> = mutableUiState.asStateFlow()

  public fun openPlayer(playerId: String) {
    if (currentPlayerId == playerId && observePlayerJob?.isActive == true) {
      return
    }

    currentPlayerId = playerId
    observePlayerJob?.cancel()
    mutableUiState.value = PlayerDetailsUiState(isLoading = true)
    observePlayerJob =
      scope.launch {
        var initialRefreshRequested = false
        observePlayerDetailsUseCase(playerId).collect { player ->
          mutableUiState.update { current ->
            current.copy(
              player = player,
              isLoading = false,
              errorMessage = if (player != null) null else current.errorMessage,
            )
          }
          if (!initialRefreshRequested && player == null) {
            initialRefreshRequested = true
            refreshInternal(playerId = playerId, showRefreshing = false)
          }
        }
      }
  }

  public fun refresh() {
    val playerId: String = currentPlayerId ?: return
    scope.launch {
      refreshInternal(playerId = playerId, showRefreshing = true)
    }
  }

  public fun clear() {
    observePlayerJob?.cancel()
    scope.cancel()
  }

  private suspend fun refreshInternal(
    playerId: String,
    showRefreshing: Boolean,
  ) {
    if (showRefreshing) {
      mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
    }
    val refreshResult: Result<Unit> = refreshPlayerDetailsUseCase(playerId)
    mutableUiState.update { current ->
      current.copy(
        isLoading = false,
        isRefreshing = false,
        errorMessage = refreshResult.exceptionOrNull()?.message,
      )
    }
  }
}
