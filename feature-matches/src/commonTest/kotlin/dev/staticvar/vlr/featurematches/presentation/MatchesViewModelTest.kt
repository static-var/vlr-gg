/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.core.network.NetworkStatus
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurematches.usecase.ObserveMatchListUseCase
import dev.staticvar.vlr.featurematches.usecase.RefreshMatchesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
  fun initDefaultsToLiveRegardlessOfSourceOrder() {
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

      assertEquals(MatchStatusFilter.Live, viewModel.uiState.value.selectedStatus)
      assertEquals(listOf("m2"), viewModel.uiState.value.pageMatches.map(MatchPreview::id))
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

    assertEquals(emptyList(), viewModel.uiState.value.pageMatches)
    assertEquals(listOf(cached), viewModel.uiState.value.matches)
    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(null, viewModel.uiState.value.errorMessage)
  }

  @Test
  fun refreshKeepsLiveSelectedWhenOnlyUpcomingMatchesExist() {
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

      assertEquals(MatchStatusFilter.Live, viewModel.uiState.value.selectedStatus)
      assertEquals(emptyList(), viewModel.uiState.value.pageMatches)
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

      assertEquals(listOf("live-earlier", "live-later"), viewModel.uiState.value.pageMatches.map(MatchPreview::id))

      viewModel.selectFilter(MatchStatusFilter.Upcoming)
      advanceUntilIdle()
      assertEquals(
        listOf("upcoming-earlier", "upcoming-later"),
        viewModel.uiState.value.pageMatches.map(MatchPreview::id),
      )

      viewModel.selectFilter(MatchStatusFilter.Completed)
      advanceUntilIdle()
      assertEquals(
        listOf("completed-newer", "completed-older"),
        viewModel.uiState.value.pageMatches.map(MatchPreview::id),
      )
    }
  }

  @Test
  fun selectFilterUpdatesSelectedPage() {
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

      assertEquals(listOf("completed-1"), viewModel.uiState.value.pageMatches.map(MatchPreview::id))
    }
  }

  @Test
  fun observedFavoriteChangesReachVisibleMatchesAndKeepEverySource() = runTest(dispatcher) {
    val match = matchPreview(id = "live-1", status = MatchStatus.LIVE)
    val repository = FakeMatchRepository(matches = listOf(match))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.pageMatches.single().isFavorite)

    val reasons = listOf(
      MatchFavoriteReason(MatchFavoriteSource.TEAM, "100t", "100 Thieves"),
      MatchFavoriteReason(MatchFavoriteSource.PLAYER, "asuna", "Asuna"),
      MatchFavoriteReason(MatchFavoriteSource.EVENT, "masters", "Masters"),
    )
    repository.matchesFlow.value = listOf(match.copy(isFavorite = true, favoriteReasons = reasons))
    advanceUntilIdle()

    assertEquals(true, viewModel.uiState.value.pageMatches.single().isFavorite)
    assertEquals(false, viewModel.uiState.value.pageMatches.single().isDirectFavorite)
    assertEquals(reasons, viewModel.uiState.value.pageMatches.single().favoriteReasons)

    repository.matchesFlow.value = listOf(match)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.pageMatches.single().isFavorite)
    assertEquals(emptyList(), viewModel.uiState.value.pageMatches.single().favoriteReasons)
  }

  @Test
  fun preservesTiesUnknownTimesAndUpcomingOverdueOrder() = runTest(dispatcher) {
    val times = listOf(
      "late" to "2025-01-01T14:00:00Z",
      "null" to null,
      "future" to "2099-01-01T10:00:00Z",
      "tie-a" to "2025-01-01T12:00:00Z",
      "invalid" to "invalid",
      "early" to "2025-01-01T10:00:00Z",
      "tie-b" to "2025-01-01T12:00:00Z",
      "blank" to "",
    )
    val expected = mapOf(
      MatchStatusFilter.Live to listOf("early", "tie-a", "tie-b", "late", "future", "null", "invalid", "blank"),
      MatchStatusFilter.Upcoming to listOf("future", "null", "invalid", "blank", "early", "tie-a", "tie-b", "late"),
      MatchStatusFilter.Completed to listOf("future", "late", "tie-a", "tie-b", "early", "null", "invalid", "blank"),
    )
    for ((filter, ids) in expected) {
      val status = when (filter) {
        MatchStatusFilter.Live -> MatchStatus.LIVE
        MatchStatusFilter.Upcoming -> MatchStatus.UPCOMING
        MatchStatusFilter.Completed -> MatchStatus.COMPLETED
      }
      val viewModel = createViewModel(FakeMatchRepository(times.map { (id, time) -> matchPreview(id, status, time) }))
      viewModel.selectFilter(filter)
      advanceUntilIdle()
      assertEquals(ids, viewModel.uiState.value.pageMatches.map(MatchPreview::id), filter.name)
    }
  }

  @Test
  fun unknownOnlyCacheIsContentWithoutStatusPages() = runTest(dispatcher) {
    val unknown = matchPreview(id = "unknown", status = MatchStatus.UNKNOWN)
    val viewModel = createViewModel(FakeMatchRepository(matches = listOf(unknown)))
    advanceUntilIdle()

    assertEquals(false, viewModel.uiState.value.isLoading)
    assertEquals(listOf(unknown), viewModel.uiState.value.matches)
    assertEquals(MatchStatusLists(), viewModel.uiState.value.statusLists)
  }

  @Test
  fun preparesStatusListsOnDefaultDispatcherInsteadOfMain() = runTest(dispatcher) {
    val default = TrackingDispatcher(StandardTestDispatcher(testScheduler))
    val readsOnDefault = mutableListOf<Boolean>()
    val source = listOf(
      matchPreview(id = "live-1", status = MatchStatus.LIVE),
      matchPreview(id = "upcoming-1", status = MatchStatus.UPCOMING),
    )
    val trackedMatches = object : AbstractList<MatchPreview>() {
      override val size: Int get() = source.size
      override fun get(index: Int): MatchPreview = source[index].also { readsOnDefault += default.isRunning }
    }
    val viewModel = createViewModel(FakeMatchRepository(trackedMatches), TestDispatcherProvider(default))
    advanceUntilIdle()

    assertEquals(listOf("live-1"), viewModel.uiState.value.statusLists.live.map(MatchPreview::id))
    assertEquals(listOf("upcoming-1"), viewModel.uiState.value.statusLists.upcoming.map(MatchPreview::id))
    assertTrue(readsOnDefault.isNotEmpty())
    assertTrue(readsOnDefault.all { it }, "match list read on Main: $readsOnDefault")
  }

  private val MatchesUiState.pageMatches: List<MatchPreview>
    get() = statusLists[selectedStatus]

  private fun createViewModel(
    repository: FakeMatchRepository,
    dispatchers: DispatcherProvider = TestDispatcherProvider(dispatcher),
  ): MatchesViewModel = MatchesViewModel(
    observeMatchListUseCase = ObserveMatchListUseCase(repository),
    refreshMatchesUseCase = RefreshMatchesUseCase(matchRepository = repository),
    networkMonitor = object : NetworkMonitor {
      override val status = MutableStateFlow(NetworkStatus.Online)
    },
    dispatchers = dispatchers,
  ).also { viewModelStore.put("viewModel-${nextViewModelKey++}", it) }

  private fun matchPreview(id: String, status: MatchStatus, time: String? = "2025-01-01T12:00:00Z"): MatchPreview =
    MatchPreview(
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
    val matchesFlow = MutableStateFlow(matches)
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

  private class TestDispatcherProvider(override val default: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = default
    override val main: CoroutineDispatcher = default
  }

  private class TrackingDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
    var isRunning: Boolean = false
      private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
      delegate.dispatch(context) {
        isRunning = true
        try {
          block.run()
        } finally {
          isRunning = false
        }
      }
    }
  }
}
