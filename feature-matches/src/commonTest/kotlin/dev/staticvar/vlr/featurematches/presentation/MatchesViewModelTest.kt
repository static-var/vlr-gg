/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchListUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchesUseCase
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
class MatchesViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val viewModelStore = ViewModelStore()
  private var nextViewModelKey = 0

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
  fun initSelectsFilterFromFirstMatchStatus() {
    runTest(dispatcher) {
      val repository =
        FakeMatchRepository(
          matches =
          listOf(
            matchPreview(id = "m1", status = MatchStatus.UPCOMING),
            matchPreview(id = "m2", status = MatchStatus.LIVE),
          ),
        )

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(MatchStatusFilter.Upcoming, viewModel.uiState.value.selectedStatus)
      assertEquals(listOf("m1"), viewModel.uiState.value.filteredMatches.map(MatchPreview::id))
      assertEquals(0, repository.refreshMatchesCallCount)
    }
  }

  @Test
  fun initKeepsLoadingEmptyCacheWithoutFetching() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository(matches = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(0, repository.refreshMatchesCallCount)
      assertEquals(true, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun selectingEmptyFilterDoesNotHideExistingCacheBehindLoading() = runTest(dispatcher) {
    val cached = matchPreview(id = "cached", status = MatchStatus.COMPLETED)
    val repository = FakeMatchRepository(matches = listOf(cached))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.selectFilter(MatchStatusFilter.Live)
    advanceUntilIdle()

    assertEquals(emptyList(), viewModel.uiState.value.filteredMatches)
    assertEquals(listOf(cached), viewModel.uiState.value.matches)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun initSelectsAvailableFilterAfterEmptyCacheRefresh() {
    runTest(dispatcher) {
      val repository =
        FakeMatchRepository(
          matches = emptyList(),
          refreshedMatches = listOf(matchPreview(id = "upcoming-1", status = MatchStatus.UPCOMING)),
        )

      val viewModel = createViewModel(repository)
      advanceUntilIdle()
      viewModel.refresh()
      advanceUntilIdle()

      assertEquals(MatchStatusFilter.Upcoming, viewModel.uiState.value.selectedStatus)
      assertEquals(listOf("upcoming-1"), viewModel.uiState.value.filteredMatches.map(MatchPreview::id))
    }
  }

  @Test
  fun ordersMatchesByStatusTiming() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository(
        matches = listOf(
          matchPreview(id = "live-later", status = MatchStatus.LIVE, time = "2025-01-01T12:00:00Z"),
          matchPreview(id = "completed-older", status = MatchStatus.COMPLETED, time = "2025-01-01T10:00:00Z"),
          matchPreview(id = "upcoming-later", status = MatchStatus.UPCOMING, time = "2025-01-01T14:00:00Z"),
          matchPreview(id = "completed-newer", status = MatchStatus.COMPLETED, time = "2025-01-01T11:00:00Z"),
          matchPreview(id = "live-earlier", status = MatchStatus.LIVE, time = "2025-01-01T09:00:00Z"),
          matchPreview(id = "upcoming-earlier", status = MatchStatus.UPCOMING, time = "2025-01-01T13:00:00Z"),
        ),
      )
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(listOf("live-earlier", "live-later"), viewModel.uiState.value.filteredMatches.map(MatchPreview::id))

      viewModel.selectFilter(MatchStatusFilter.Upcoming)
      advanceUntilIdle()
      assertEquals(listOf("upcoming-earlier", "upcoming-later"), viewModel.uiState.value.filteredMatches.map(MatchPreview::id))

      viewModel.selectFilter(MatchStatusFilter.Completed)
      advanceUntilIdle()
      assertEquals(listOf("completed-newer", "completed-older"), viewModel.uiState.value.filteredMatches.map(MatchPreview::id))
    }
  }

  @Test
  fun selectFilterUpdatesFilteredMatches() {
    runTest(dispatcher) {
      val repository =
        FakeMatchRepository(
          matches =
          listOf(
            matchPreview(id = "live-1", status = MatchStatus.LIVE),
            matchPreview(id = "upcoming-1", status = MatchStatus.UPCOMING),
            matchPreview(id = "completed-1", status = MatchStatus.COMPLETED),
          ),
        )
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.selectFilter(MatchStatusFilter.Completed)
      advanceUntilIdle()

      assertEquals(listOf("completed-1"), viewModel.uiState.value.filteredMatches.map(MatchPreview::id))
    }
  }

  private fun createViewModel(repository: FakeMatchRepository): MatchesViewModel = MatchesViewModel(
    observeMatchListUseCase = ObserveMatchListUseCase(repository),
    refreshMatchesUseCase = RefreshMatchesUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
  ).also { viewModelStore.put("viewModel-${nextViewModelKey++}", it) }

  private fun matchPreview(
    id: String,
    status: MatchStatus,
    time: String? = "2025-01-01T12:00:00Z",
  ): MatchPreview = MatchPreview(
    id = id,
    event = "Masters",
    series = "Bo3",
    status = status,
    team1 = teamPreview(name = "Alpha"),
    team2 = teamPreview(name = "Bravo"),
    time = time,
    eventId = "event-1",
  )

  private fun teamPreview(name: String): TeamPreview = TeamPreview(
    id = name.lowercase(),
    name = name,
    region = "EMEA",
    img = "",
    score = null,
    isWinner = null,
  )

  private class FakeMatchRepository(
    matches: List<MatchPreview>,
    private val refreshedMatches: List<MatchPreview> = matches,
  ) : MatchRepository {
    private val matchesFlow = MutableStateFlow(matches)
    var refreshMatchesCallCount: Int = 0
      private set

    override fun getMatches(): Flow<List<MatchPreview>> = matchesFlow

    override fun getMatchDetails(matchId: String): Flow<MatchDetails?> = flowOf(null)

    override suspend fun addToFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatches(): Result<Unit> {
      refreshMatchesCallCount += 1
      matchesFlow.value = refreshedMatches
      return Result.success(Unit)
    }

    override suspend fun refreshMatchDetails(matchId: String): Result<Unit> = Result.success(Unit)
  }
}
