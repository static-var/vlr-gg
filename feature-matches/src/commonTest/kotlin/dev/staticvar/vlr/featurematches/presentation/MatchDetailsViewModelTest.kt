/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchDetailsUseCase
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
class MatchDetailsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

  @Test
  fun openMatchRefreshesWhenDetailsAreMissing() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository()
      val viewModel = createViewModel(repository)

      viewModel.openMatch("match-1")
      advanceUntilIdle()

      assertEquals(listOf("match-1"), repository.refreshDetailRequests)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  @Test
  fun openMatchSameIdDoesNotRequeryWhileActive() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository(details = matchDetails("match-1"))
      val viewModel = createViewModel(repository)

      viewModel.openMatch("match-1")
      advanceUntilIdle()
      viewModel.openMatch("match-1")
      advanceUntilIdle()

      assertEquals(listOf("match-1"), repository.observedMatchIds)
      assertEquals(emptyList(), repository.refreshDetailRequests)

      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeMatchRepository): MatchDetailsViewModel = MatchDetailsViewModel(
    observeMatchDetailsUseCase = ObserveMatchDetailsUseCase(repository),
    refreshMatchDetailsUseCase = RefreshMatchDetailsUseCase(repository),
    dispatchers = dispatchers,
  )

  private fun matchDetails(matchId: String): MatchDetails = MatchDetails(
    id = matchId,
    event = EventInfo(
      id = "event-1",
      name = "Masters",
      series = "Bo3",
      stage = "Playoffs",
      img = "",
      date = "Today",
      patch = null,
      status = "LIVE",
    ),
    head2head = emptyList(),
    note = "",
    score = "0:0",
    teams = emptyList(),
    bans = emptyList(),
    videos = MatchVideos(streams = emptyList(), vods = emptyList()),
    matchData = emptyList(),
    mapCount = 0,
  )

  private class FakeMatchRepository(details: MatchDetails? = null) : MatchRepository {
    val observedMatchIds: MutableList<String> = mutableListOf()
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    private val detailsByMatchId: MutableMap<String, MutableStateFlow<MatchDetails?>> = mutableMapOf()

    init {
      detailsByMatchId["match-1"] = MutableStateFlow(details)
    }

    override fun getMatches(): Flow<List<MatchPreview>> = flowOf(emptyList())

    override fun getMatchDetails(matchId: String): Flow<MatchDetails?> {
      observedMatchIds += matchId
      return detailsByMatchId.getOrPut(matchId) { MutableStateFlow(null) }
    }

    override suspend fun addToFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatches(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatchDetails(matchId: String): Result<Unit> {
      refreshDetailRequests += matchId
      return Result.success(Unit)
    }
  }

  private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
