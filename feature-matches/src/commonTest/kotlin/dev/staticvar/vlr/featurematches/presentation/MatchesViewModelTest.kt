/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchListUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MatchesViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

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
      assertEquals(0, repository.refreshMatchesCallCount)

      viewModel.clear()
    }
  }

  @Test
  fun initRefreshesWhenMatchesAreEmpty() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository(matches = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(1, repository.refreshMatchesCallCount)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeMatchRepository): MatchesViewModel = MatchesViewModel(
    observeMatchListUseCase = ObserveMatchListUseCase(repository),
    refreshMatchesUseCase = RefreshMatchesUseCase(repository),
    dispatchers = dispatchers,
  )

  private fun matchPreview(id: String, status: MatchStatus): MatchPreview = MatchPreview(
    id = id,
    event = "Masters",
    series = "Bo3",
    status = status,
    team1 = teamPreview(name = "Alpha"),
    team2 = teamPreview(name = "Bravo"),
    time = "12:00",
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

  private class FakeMatchRepository(matches: List<MatchPreview>) : MatchRepository {
    private val matchesFlow = MutableStateFlow(matches)
    var refreshMatchesCallCount: Int = 0
      private set

    override fun getMatches(): Flow<List<MatchPreview>> = matchesFlow

    override fun getMatchDetails(matchId: String): Flow<MatchDetails?> = flowOf(null)

    override suspend fun addToFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatches(): Result<Unit> {
      refreshMatchesCallCount += 1
      return Result.success(Unit)
    }

    override suspend fun refreshMatchDetails(matchId: String): Result<Unit> = Result.success(Unit)
  }

  private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
