/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurerankings.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.domain.model.RegionalRanking
import dev.staticvar.vlr.domain.model.TeamRanking
import dev.staticvar.vlr.domain.repository.RankingsRepository
import dev.staticvar.vlr.featurerankings.usecase.ObserveRankingsUseCase
import dev.staticvar.vlr.featurerankings.usecase.RefreshRankingsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class RankingsViewModelTest {
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
  fun initSelectsFirstRegionWhenRankingsExist() {
    runTest(dispatcher) {
      val repository =
        FakeRankingsRepository(
          rankings =
          listOf(
            ranking(region = "EMEA", teamName = "FNATIC", rank = 1),
            ranking(region = "Americas", teamName = "G2", rank = 1),
          ),
        )

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals("EMEA", viewModel.uiState.value.selectedRegion)
      assertEquals(2, viewModel.uiState.value.regions.size)
    }
  }

  @Test
  fun initKeepsLoadingUntilInitialRefreshCompletes() {
    runTest(dispatcher) {
      val repository = FakeRankingsRepository(rankings = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(0, repository.refreshCallCount)
      assertEquals(true, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun refreshCoalescesAndKeepsCacheOnFailure() = runTest(dispatcher) {
    val cached = listOf(ranking("EMEA", "FNATIC", 1))
    val repository = FakeRankingsRepository(cached)
    val gate = CompletableDeferred<Unit>()
    repository.refreshGate = gate
    repository.refreshResult = Result.failure(IllegalStateException("offline"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.refresh()
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(1, repository.refreshCallCount)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(cached, viewModel.uiState.value.regions)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("offline", viewModel.uiState.value.errorMessage)
    assertEquals(cached, viewModel.uiState.value.regions)
  }

  @Test
  fun chosenRegionSurvivesDatabaseUpdatesAndRefresh() = runTest(dispatcher) {
    val emea = ranking("EMEA", "FNATIC", 1)
    val americas = ranking("Americas", "G2", 1)
    val repository = FakeRankingsRepository(listOf(emea, americas))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.selectRegion("Americas")
    repository.rankingsFlow.value = listOf(americas, emea)
    viewModel.refresh()
    advanceUntilIdle()

    assertEquals("Americas", viewModel.uiState.value.selectedRegion)
    assertEquals(listOf(americas, emea), viewModel.uiState.value.regions)
  }

  private fun createViewModel(repository: FakeRankingsRepository): RankingsViewModel = RankingsViewModel(
    observeRankingsUseCase = ObserveRankingsUseCase(repository),
    refreshRankingsUseCase = RefreshRankingsUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
  ).also { viewModelStore.put("viewModel", it) }

  private fun ranking(region: String, teamName: String, rank: Int): RegionalRanking = RegionalRanking(
    region = region,
    teams =
    listOf(
      TeamRanking(
        teamId = "$region-$rank",
        teamName = teamName,
        teamLogo = "",
        country = region,
        rank = rank,
        points = "100",
      ),
    ),
  )

  private class FakeRankingsRepository(rankings: List<RegionalRanking>) : RankingsRepository {
    var refreshGate: CompletableDeferred<Unit>? = null
    var refreshResult: Result<Unit> = Result.success(Unit)
    val rankingsFlow = MutableStateFlow(rankings)
    var refreshCallCount: Int = 0
      private set

    override fun getAllRankings(): Flow<List<RegionalRanking>> = rankingsFlow

    override fun getRankingsByRegion(region: String): Flow<RegionalRanking?> =
      MutableStateFlow(rankingsFlow.value.firstOrNull { it.region == region })

    override suspend fun refreshRankings(): Result<Unit> {
      refreshCallCount += 1
      refreshGate?.await()
      return refreshResult
    }
  }
}
