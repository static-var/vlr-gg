/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MapData
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchDetailsUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchDetailsUseCase
import kotlinx.coroutines.CompletableDeferred
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
      val repository = FakeMatchRepository(details = matchDetails("match-1", hasDetails = true))
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

  @Test
  fun openMatchRefreshesWhenOnlyPreviewRowIsCached() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository(details = matchDetails("match-1", hasDetails = false))
      val viewModel = createViewModel(repository)

      viewModel.openMatch("match-1")
      advanceUntilIdle()

      assertEquals(listOf("match-1"), repository.refreshDetailRequests)

      viewModel.clear()
    }
  }

  @Test
  fun openMatchRefreshesCachedShellAfterMissingRowRefresh() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository()
      val viewModel = createViewModel(repository)

      viewModel.openMatch("match-1")
      advanceUntilIdle()
      repository.publishDetails(matchDetails("match-1", hasDetails = false))
      advanceUntilIdle()

      assertEquals(listOf("match-1", "match-1"), repository.refreshDetailRequests)

      viewModel.clear()
    }
  }

  @Test
  fun clearKeepsInFlightDetailRefreshAliveForCachePopulation() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository(details = matchDetails("match-1", hasDetails = false))
      repository.blockDetailRefresh = true
      val viewModel = createViewModel(repository)

      viewModel.openMatch("match-1")
      advanceUntilIdle()
      repository.refreshStarted.await()

      viewModel.clear()
      repository.allowRefresh.complete(Unit)
      advanceUntilIdle()

      assertEquals(true, repository.refreshCompleted)
    }
  }

  @Test
  fun openMatchKeepsLoadingWhileCachedShellRefreshIsInFlight() {
    runTest(dispatcher) {
      val repository = FakeMatchRepository(details = matchDetails("match-1", hasDetails = false))
      repository.blockDetailRefresh = true
      val viewModel = createViewModel(repository)

      viewModel.openMatch("match-1")
      advanceUntilIdle()
      repository.refreshStarted.await()

      assertEquals(true, viewModel.uiState.value.isLoading)

      repository.allowRefresh.complete(Unit)
      advanceUntilIdle()

      assertEquals(true, viewModel.uiState.value.isLoading)

      repository.publishDetails(matchDetails("match-1", hasDetails = true))
      advanceUntilIdle()

      assertEquals(false, viewModel.uiState.value.isLoading)
      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeMatchRepository): MatchDetailsViewModel = MatchDetailsViewModel(
    observeMatchDetailsUseCase = ObserveMatchDetailsUseCase(repository),
    refreshMatchDetailsUseCase = RefreshMatchDetailsUseCase(repository),
    dispatchers = dispatchers,
  )

  private fun matchDetails(matchId: String, hasDetails: Boolean = false): MatchDetails = MatchDetails(
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
    matchData = if (hasDetails) listOf(mapData()) else emptyList(),
    mapCount = if (hasDetails) 1 else 0,
  )

  private fun mapData(): MapData = MapData(
    map = "Lotus",
    members = emptyList(),
    teams = emptyList(),
    rounds = emptyList(),
  )

  private class FakeMatchRepository(details: MatchDetails? = null) : MatchRepository {
    val observedMatchIds: MutableList<String> = mutableListOf()
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    var blockDetailRefresh: Boolean = false
    val refreshStarted: CompletableDeferred<Unit> = CompletableDeferred()
    val allowRefresh: CompletableDeferred<Unit> = CompletableDeferred()
    var refreshCompleted: Boolean = false
    private val detailsByMatchId: MutableMap<String, MutableStateFlow<MatchDetails?>> = mutableMapOf()

    init {
      detailsByMatchId["match-1"] = MutableStateFlow(details)
    }

    override fun getMatches(): Flow<List<MatchPreview>> = flowOf(emptyList())

    override fun getMatchDetails(matchId: String): Flow<MatchDetails?> {
      observedMatchIds += matchId
      return detailsByMatchId.getOrPut(matchId) { MutableStateFlow(null) }
    }

    fun publishDetails(details: MatchDetails?) {
      detailsByMatchId.getOrPut("match-1") { MutableStateFlow(null) }.value = details
    }

    override suspend fun addToFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatches(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatchDetails(matchId: String): Result<Unit> {
      refreshDetailRequests += matchId
      refreshStarted.complete(Unit)
      if (blockDetailRefresh) {
        allowRefresh.await()
      }
      refreshCompleted = true
      return Result.success(Unit)
    }
  }

  private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
