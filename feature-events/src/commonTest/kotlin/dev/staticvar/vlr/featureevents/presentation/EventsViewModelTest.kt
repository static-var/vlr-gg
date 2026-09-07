/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import androidx.lifecycle.ViewModelStore
import dev.staticvar.vlr.core.network.NetworkMonitor
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.featureevents.usecase.ObserveEventListUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventsUseCase
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
class EventsViewModelTest {
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
  fun pausedAndUnknownEventsRemainAccessibleOutsideUpcoming() = runTest(dispatcher) {
    val repository = FakeEventRepository(
      events = listOf(
        eventPreview(id = "paused", status = EventStatus.PAUSED),
        eventPreview(id = "unknown", status = EventStatus.UNKNOWN),
        eventPreview(id = "upcoming", status = EventStatus.UPCOMING),
      ),
    )
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    assertEquals(EventStatusFilter.Paused, viewModel.uiState.value.selectedStatus)
    assertEquals(listOf("paused"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))
    viewModel.selectFilter(EventStatusFilter.Unknown)
    advanceUntilIdle()
    assertEquals(listOf("unknown"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))
    viewModel.selectFilter(EventStatusFilter.Upcoming)
    advanceUntilIdle()
    assertEquals(listOf("upcoming"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))
  }

  @Test
  fun initSelectsFilterFromFirstEventStatus() {
    runTest(dispatcher) {
      val repository =
        FakeEventRepository(
          events = listOf(eventPreview(id = "e1", status = EventStatus.COMPLETED)),
        )

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(EventStatusFilter.Completed, viewModel.uiState.value.selectedStatus)
      assertEquals(listOf("e1"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))
      assertEquals(0, repository.refreshEventsCallCount)
    }
  }

  @Test
  fun emptyOfflineCacheKeepsLoadingWithoutStartingNetwork() {
    runTest(dispatcher) {
      val repository = FakeEventRepository(events = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(0, repository.refreshEventsCallCount)
      assertEquals(true, viewModel.uiState.value.isLoading)
    }
  }

  @Test
  fun initSelectsAvailableFilterAfterEmptyCacheRefresh() {
    runTest(dispatcher) {
      val repository =
        FakeEventRepository(
          events = emptyList(),
          refreshedEvents = listOf(eventPreview(id = "upcoming-1", status = EventStatus.UPCOMING)),
        )

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.refresh()
      advanceUntilIdle()

      assertEquals(EventStatusFilter.Upcoming, viewModel.uiState.value.selectedStatus)
      assertEquals(listOf("upcoming-1"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))
    }
  }

  @Test
  fun ordersEventsByStatusTiming() {
    runTest(dispatcher) {
      val repository = FakeEventRepository(
        events = listOf(
          eventPreview(id = "ongoing-later", status = EventStatus.ONGOING, dates = "Mar 8 - Mar 16"),
          eventPreview(id = "completed-older", status = EventStatus.COMPLETED, dates = "Feb 1 - Feb 5"),
          eventPreview(id = "upcoming-later", status = EventStatus.UPCOMING, dates = "Apr 8 - Apr 16"),
          eventPreview(id = "completed-newer", status = EventStatus.COMPLETED, dates = "Mar 1 - Mar 7"),
          eventPreview(id = "ongoing-earlier", status = EventStatus.ONGOING, dates = "Mar 1 - Mar 7"),
          eventPreview(id = "upcoming-earlier", status = EventStatus.UPCOMING, dates = "Apr 1 - Apr 7"),
        ),
      )
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(listOf("ongoing-earlier", "ongoing-later"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))

      viewModel.selectFilter(EventStatusFilter.Upcoming)
      advanceUntilIdle()
      assertEquals(listOf("upcoming-earlier", "upcoming-later"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))

      viewModel.selectFilter(EventStatusFilter.Completed)
      advanceUntilIdle()
      assertEquals(listOf("completed-newer", "completed-older"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))
    }
  }

  @Test
  fun selectFilterUpdatesFilteredEvents() {
    runTest(dispatcher) {
      val repository =
        FakeEventRepository(
          events =
          listOf(
            eventPreview(id = "ongoing-1", status = EventStatus.ONGOING),
            eventPreview(id = "upcoming-1", status = EventStatus.UPCOMING),
            eventPreview(id = "completed-1", status = EventStatus.COMPLETED),
          ),
        )
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.selectFilter(EventStatusFilter.Upcoming)
      advanceUntilIdle()

      assertEquals(listOf("upcoming-1"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))
    }
  }

  @Test
  fun refreshKeepsCacheVisibleAndReportsFailure() = runTest(dispatcher) {
    val cached = listOf(eventPreview(id = "cached", status = EventStatus.ONGOING))
    val repository = FakeEventRepository(cached)
    repository.blockRefresh = true
    repository.refreshResult = Result.failure(IllegalStateException("Offline"))
    val viewModel = createViewModel(repository)
    advanceUntilIdle()

    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(1, repository.refreshEventsCallCount)
    assertEquals(cached, viewModel.uiState.value.events)
    assertEquals(true, viewModel.uiState.value.isRefreshing)
    assertEquals(false, viewModel.uiState.value.isLoading)

    repository.allowRefresh.complete(Unit)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.isRefreshing)
    assertEquals("Offline", viewModel.uiState.value.errorMessage)
    assertEquals(cached, viewModel.uiState.value.events)
  }

  @Test
  fun emptyRefreshKeepsSelectedPausedOrUnknownTabVisible() = runTest(dispatcher) {
    listOf(EventStatus.PAUSED to EventStatusFilter.Paused, EventStatus.UNKNOWN to EventStatusFilter.Unknown)
      .forEach { (status, filter) ->
        val repository = FakeEventRepository(
          events = listOf(eventPreview(id = "event", status = status)),
          refreshedEvents = emptyList(),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(filter, state.selectedStatus)
        assertTrue(state.filteredEvents.isEmpty())
        assertTrue(filter in state.visibleStatusFilters)

      }
  }

  private fun createViewModel(repository: FakeEventRepository): EventsViewModel = EventsViewModel(
    observeEventListUseCase = ObserveEventListUseCase(repository),
    refreshEventsUseCase = RefreshEventsUseCase(repository),
    networkMonitor = object : NetworkMonitor {
      override val isOnline = MutableStateFlow(true)
    },
  ).also { viewModelStore.put("viewModel-${nextViewModelKey++}", it) }

  private fun eventPreview(
    id: String,
    status: EventStatus,
    dates: String = "Mar 1 - Mar 7",
  ): EventPreview = EventPreview(
    id = id,
    title = "Champions",
    status = status,
    prize = "$" + "100k",
    dates = dates,
    region = "Global",
    logoUrl = "",
  )

  private class FakeEventRepository(
    events: List<EventPreview>,
    private val refreshedEvents: List<EventPreview> = events,
  ) : EventRepository {
    private val eventsFlow = MutableStateFlow(events)
    var blockRefresh = false
    val allowRefresh = CompletableDeferred<Unit>()
    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshEventsCallCount: Int = 0
      private set

    override fun getEvents(): Flow<List<EventPreview>> = eventsFlow

    override fun getEventDetails(eventId: String): Flow<EventDetails?> = flowOf(null)

    override suspend fun addToFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEvents(): Result<Unit> {
      refreshEventsCallCount += 1
      if (blockRefresh) allowRefresh.await()
      if (refreshResult.isSuccess) eventsFlow.value = refreshedEvents
      return refreshResult
    }

    override suspend fun refreshEventDetails(eventId: String): Result<Unit> = Result.success(Unit)
  }
}
