/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureplayer.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.featureplayer.usecase.ObservePlayerDetailsUseCase
import dev.staticvar.vlr.featureplayer.usecase.RefreshPlayerDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerDetailsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val viewModelStore = ViewModelStore()

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    viewModelStore.clear()
    Dispatchers.resetMain()
  }

  @Test
  fun initFinishesLoadingWithoutNetworkWhenDetailsAreMissing() {
    runTest(dispatcher) {
      val repository = FakePlayerRepository()
      val viewModel = createViewModel(repository)

      advanceUntilIdle()

      assertEquals(emptyList(), repository.refreshDetailRequests)
      assertEquals(false, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun initObservesCachedProfileOnce() {
    runTest(dispatcher) {
      val repository = FakePlayerRepository(player = playerInfo("player-1"))
      val viewModel = createViewModel(repository)

      advanceUntilIdle()

      assertEquals(playerInfo("player-1"), viewModel.uiState.value.player)
      assertEquals(listOf("player-1"), repository.observedPlayerIds)
      assertEquals(emptyList(), repository.refreshDetailRequests)
    }
  }

  @Test
  fun refreshCoalescesAndKeepsCacheOnFailure() = runTest(dispatcher) {
    val cached = playerInfo("player-1")
    val repository = FakePlayerRepository(player = cached)
    val gate = CompletableDeferred<Unit>()
    repository.refreshGate = gate
    repository.refreshResult = Result.failure(IllegalStateException("offline"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.refresh()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("player-1"), repository.refreshDetailRequests)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(cached, viewModel.uiState.value.player)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("offline", viewModel.uiState.value.errorMessage)
    assertEquals(cached, viewModel.uiState.value.player)
  }

  @Test
  fun clearingViewModelStoreCancelsItsRefresh() = runTest(dispatcher) {
    val repository = FakePlayerRepository(player = playerInfo("player-1"))
    repository.refreshGate = CompletableDeferred()
    val viewModel = createViewModel(repository)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("player-1"), repository.refreshDetailRequests)

    viewModelStore.clear()
    advanceUntilIdle()
    assertEquals(true, repository.refreshCancelled)
    assertEquals(listOf("player-1"), repository.refreshDetailRequests)
  }

  private fun createViewModel(repository: FakePlayerRepository): PlayerDetailsViewModel = PlayerDetailsViewModel(
    playerId = "player-1",
    observePlayerDetailsUseCase = ObservePlayerDetailsUseCase(repository),
    refreshPlayerDetailsUseCase = RefreshPlayerDetailsUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
  ).also { viewModelStore.put("viewModel", it) }

  private fun playerInfo(playerId: String): PlayerInfo = PlayerInfo(
    id = playerId,
    name = "Boaster",
    alias = "boaster",
    realName = null,
    country = "UK",
    imageUrl = "",
    twitterUrl = null,
    twitchUrl = null,
    totalWinnings = 0.0,
    currentTeam = null,
    pastTeams = emptyList(),
    agentStats = emptyList(),
  )

  private class FakePlayerRepository(player: PlayerInfo? = null) : PlayerRepository {
    var refreshCancelled: Boolean = false
    var refreshGate: CompletableDeferred<Unit>? = null
    var refreshResult: Result<Unit> = Result.success(Unit)
    val observedPlayerIds: MutableList<String> = mutableListOf()
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    private val detailsByPlayerId: MutableMap<String, MutableStateFlow<PlayerInfo?>> = mutableMapOf()

    init {
      detailsByPlayerId["player-1"] = MutableStateFlow(player)
    }

    override fun getPlayerInTeam(teamId: String): Flow<List<PlayerInfo?>> = flowOf(emptyList())

    override fun getPlayerDetails(playerId: String): Flow<PlayerInfo?> {
      observedPlayerIds += playerId
      return detailsByPlayerId.getOrPut(playerId) { MutableStateFlow(null) }
    }

    override suspend fun addToFavorites(playerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(playerId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshPlayerDetails(playerId: String): Result<Unit> {
      refreshDetailRequests += playerId
      try {
        refreshGate?.await()
        return refreshResult
      } catch (cancelled: CancellationException) {
        refreshCancelled = true
        throw cancelled
      }
    }
  }
}
