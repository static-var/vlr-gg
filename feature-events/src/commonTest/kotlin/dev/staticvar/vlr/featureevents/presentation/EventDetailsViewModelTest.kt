/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventMatchTeam
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.featureevents.usecase.ObserveEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventDetailsUseCase
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
class EventDetailsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

  @Test
  fun openEventRefreshesWhenDetailsAreMissing() {
    runTest(dispatcher) {
      val repository = FakeEventRepository()
      val viewModel = createViewModel(repository)

      viewModel.openEvent("event-1")
      advanceUntilIdle()

      assertEquals(listOf("event-1"), repository.refreshDetailRequests)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
    }
  }

  @Test
  fun openEventRefreshesWhenCachedOverviewHasNoDetailSlices() {
    runTest(dispatcher) {
      val repository = FakeEventRepository(
        details = EventDetails(
          id = "event-1",
          title = "Champions",
          subtitle = "Stage 1",
          status = EventStatus.ONGOING,
          prize = "$" + "100k",
          dates = "Mar 1 - Mar 7",
          region = "Global",
          logoUrl = "",
          prizes = emptyList(),
          teams = emptyList(),
          matches = emptyList(),
          standings = emptyList(),
        ),
      )
      val viewModel = createViewModel(repository)

      viewModel.openEvent("event-1")
      advanceUntilIdle()

      assertEquals(listOf("event-1"), repository.refreshDetailRequests)

      viewModel.clear()
    }
  }

  @Test
  fun openEventRefreshesCachedShellAfterMissingRowRefresh() {
    runTest(dispatcher) {
      val repository = FakeEventRepository()
      val viewModel = createViewModel(repository)

      viewModel.openEvent("event-1")
      advanceUntilIdle()
      repository.publishDetails(eventDetailsWithoutSlices())
      advanceUntilIdle()

      assertEquals(listOf("event-1", "event-1"), repository.refreshDetailRequests)

      viewModel.clear()
    }
  }

  @Test
  fun clearKeepsInFlightDetailRefreshAliveForCachePopulation() {
    runTest(dispatcher) {
      val repository = FakeEventRepository(
        details = EventDetails(
          id = "event-1",
          title = "Champions",
          subtitle = "Stage 1",
          status = EventStatus.ONGOING,
          prize = "$" + "100k",
          dates = "Mar 1 - Mar 7",
          region = "Global",
          logoUrl = "",
          prizes = emptyList(),
          teams = emptyList(),
          matches = emptyList(),
          standings = emptyList(),
        ),
      )
      repository.blockDetailRefresh = true
      val viewModel = createViewModel(repository)

      viewModel.openEvent("event-1")
      advanceUntilIdle()
      repository.refreshStarted.await()

      viewModel.clear()
      repository.allowRefresh.complete(Unit)
      advanceUntilIdle()

      assertEquals(true, repository.refreshCompleted)
    }
  }

  @Test
  fun openEventKeepsLoadingWhileCachedShellRefreshIsInFlight() {
    runTest(dispatcher) {
      val repository = FakeEventRepository(details = eventDetailsWithoutSlices())
      repository.blockDetailRefresh = true
      val viewModel = createViewModel(repository)

      viewModel.openEvent("event-1")
      advanceUntilIdle()
      repository.refreshStarted.await()

      assertEquals(true, viewModel.uiState.value.isLoading)

      repository.allowRefresh.complete(Unit)
      advanceUntilIdle()

      assertEquals(true, viewModel.uiState.value.isLoading)

      repository.publishDetails(eventDetailsWithSlices())
      advanceUntilIdle()

      assertEquals(false, viewModel.uiState.value.isLoading)
      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeEventRepository): EventDetailsViewModel = EventDetailsViewModel(
    observeEventDetailsUseCase = ObserveEventDetailsUseCase(repository),
    refreshEventDetailsUseCase = RefreshEventDetailsUseCase(repository),
    dispatchers = dispatchers,
  )

  private class FakeEventRepository(details: EventDetails? = null) : EventRepository {
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    var blockDetailRefresh: Boolean = false
    val refreshStarted: CompletableDeferred<Unit> = CompletableDeferred()
    val allowRefresh: CompletableDeferred<Unit> = CompletableDeferred()
    var refreshCompleted: Boolean = false
    private val detailsByEventId: MutableMap<String, MutableStateFlow<EventDetails?>> = mutableMapOf()

    init {
      detailsByEventId["event-1"] = MutableStateFlow(details)
    }

    override fun getEvents(): Flow<List<EventPreview>> = flowOf(emptyList())

    override fun getEventDetails(eventId: String): Flow<EventDetails?> = detailsByEventId.getOrPut(eventId) {
      MutableStateFlow(eventDetailsWithoutSlices(id = eventId))
    }

    fun publishDetails(details: EventDetails?) {
      detailsByEventId.getOrPut("event-1") { MutableStateFlow(null) }.value = details
    }

    override suspend fun addToFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEvents(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEventDetails(eventId: String): Result<Unit> {
      refreshDetailRequests += eventId
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

private fun eventDetailsWithoutSlices(id: String = "event-1"): EventDetails = EventDetails(
  id = id,
  title = "Champions",
  subtitle = "Stage 1",
  status = EventStatus.ONGOING,
  prize = "$" + "100k",
  dates = "Mar 1 - Mar 7",
  region = "Global",
  logoUrl = "",
  prizes = emptyList(),
  teams = emptyList(),
  matches = emptyList(),
  standings = emptyList(),
)

private fun eventDetailsWithSlices(id: String = "event-1"): EventDetails = eventDetailsWithoutSlices(id = id).copy(
  matches = listOf(
    EventMatch(
      matchId = "match-1",
      time = "10:00",
      date = "Today",
      eta = null,
      status = "upcoming",
      teams = listOf(
        EventMatchTeam(name = "FNATIC", region = "EMEA", score = null),
        EventMatchTeam(name = "Sentinels", region = "Americas", score = null),
      ),
      round = "Upper final",
      stage = "Playoffs",
    ),
  ),
)
