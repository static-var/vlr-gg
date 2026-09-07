/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureteam.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.TeamRepository
import dev.staticvar.vlr.featureteam.usecase.ObserveTeamDetailsUseCase
import dev.staticvar.vlr.featureteam.usecase.RefreshTeamDetailsUseCase
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TeamDetailsViewModelTest {
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
  fun initKeepsLoadingUntilInitialRefreshCompletes() {
    runTest(dispatcher) {
      val repository = FakeTeamRepository()
      val viewModel = createViewModel(repository)

      advanceUntilIdle()

      assertEquals(emptyList(), repository.refreshDetailRequests)
      assertEquals(true, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun missingDetailsStayLoadingUntilRequestFailsThenRetryCanFinishEmpty() = runTest(dispatcher) {
    val repository = FakeTeamRepository()
    val gate = CompletableDeferred<Unit>()
    val failure = IllegalStateException("Team request failed", IllegalArgumentException("Invalid response"))
    repository.refreshGate = gate
    repository.refreshResult = Result.failure(failure)
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.team)
    assertEquals("Team request failed", viewModel.uiState.value.errorMessage)
    assertEquals(failure.stackTraceToString(), viewModel.uiState.value.errorDetails)

    repository.refreshGate = CompletableDeferred()
    repository.refreshResult = Result.success(Unit)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
    assertEquals(null, viewModel.uiState.value.errorDetails)
    repository.refreshGate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.team)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun initObservesCachedProfileOnce() {
    runTest(dispatcher) {
      val repository = FakeTeamRepository(team = teamInfo("team-1"))
      val viewModel = createViewModel(repository)

      advanceUntilIdle()

      assertEquals(teamInfo("team-1"), viewModel.uiState.value.team)
      assertEquals(listOf("team-1"), repository.observedTeamIds)
      assertEquals(emptyList(), repository.refreshDetailRequests)
    }
  }

  @Test
  fun refreshCoalescesAndKeepsCacheOnFailure() = runTest(dispatcher) {
    val cached = teamInfo("team-1")
    val repository = FakeTeamRepository(team = cached)
    val gate = CompletableDeferred<Unit>()
    repository.refreshGate = gate
    repository.refreshResult = Result.failure(IllegalStateException("offline"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.refresh()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), repository.refreshDetailRequests)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(cached, viewModel.uiState.value.team)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("offline", viewModel.uiState.value.errorMessage)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.errorDetails.orEmpty().contains("IllegalStateException: offline"))
    assertEquals(cached, viewModel.uiState.value.team)
  }

  @Test
  fun clearingViewModelStoreCancelsItsRefresh() = runTest(dispatcher) {
    val repository = FakeTeamRepository(team = teamInfo("team-1"))
    repository.refreshGate = CompletableDeferred()
    val viewModel = createViewModel(repository)
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(listOf("team-1"), repository.refreshDetailRequests)

    viewModelStore.clear()
    advanceUntilIdle()
    assertEquals(true, repository.refreshCancelled)
    assertEquals(listOf("team-1"), repository.refreshDetailRequests)
  }

  private fun createViewModel(repository: FakeTeamRepository): TeamDetailsViewModel = TeamDetailsViewModel(
    teamId = "team-1",
    observeTeamDetailsUseCase = ObserveTeamDetailsUseCase(repository),
    refreshTeamDetailsUseCase = RefreshTeamDetailsUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
  ).also { viewModelStore.put("viewModel", it) }

  private fun teamInfo(teamId: String): TeamInfo = TeamInfo(
    id = teamId,
    name = "FNATIC",
    tag = "FNC",
    logoUrl = "",
    region = "EMEA",
    country = "EU",
    rank = 1,
    website = null,
    twitter = null,
    roster = emptyList(),
    upcomingMatches = emptyList(),
    completedMatches = emptyList(),
  )

  private class FakeTeamRepository(team: TeamInfo? = null) : TeamRepository {
    var refreshCancelled: Boolean = false
    var refreshGate: CompletableDeferred<Unit>? = null
    var refreshResult: Result<Unit> = Result.success(Unit)
    val observedTeamIds: MutableList<String> = mutableListOf()
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    private val detailsByTeamId: MutableMap<String, MutableStateFlow<TeamInfo?>> = mutableMapOf()

    init {
      detailsByTeamId["team-1"] = MutableStateFlow(team)
    }

    override fun getTeams(): Flow<List<TeamInfo>> = flowOf(emptyList())

    override fun getTeamDetails(teamId: String): Flow<TeamInfo?> {
      observedTeamIds += teamId
      return detailsByTeamId.getOrPut(teamId) { MutableStateFlow(null) }
    }

    override fun getTeamsByRegion(region: String): Flow<List<TeamInfo>> = flowOf(emptyList())

    override suspend fun addToFavorites(teamId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(teamId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> {
      refreshDetailRequests += teamId
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
