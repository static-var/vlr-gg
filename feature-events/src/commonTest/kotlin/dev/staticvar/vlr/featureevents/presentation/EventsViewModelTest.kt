/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.featureevents.usecase.ObserveEventListUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventsUseCase
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
class EventsViewModelTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)

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
    viewModel.clear()
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
      assertEquals(1, repository.refreshEventsCallCount)

      viewModel.clear()
    }
  }

  @Test
  fun initRefreshesWhenEventsAreEmpty() {
    runTest(dispatcher) {
      val repository = FakeEventRepository(events = emptyList())

      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(1, repository.refreshEventsCallCount)
      assertEquals(false, viewModel.uiState.value.isLoading)

      viewModel.clear()
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

      assertEquals(EventStatusFilter.Upcoming, viewModel.uiState.value.selectedStatus)
      assertEquals(listOf("upcoming-1"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))

      viewModel.clear()
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
      assertEquals(listOf("upcoming-earlier", "upcoming-later"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))

      viewModel.selectFilter(EventStatusFilter.Completed)
      assertEquals(listOf("completed-newer", "completed-older"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))

      viewModel.clear()
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

      assertEquals(listOf("upcoming-1"), viewModel.uiState.value.filteredEvents.map(EventPreview::id))

      viewModel.clear()
    }
  }

  private fun createViewModel(repository: FakeEventRepository): EventsViewModel = EventsViewModel(
    observeEventListUseCase = ObserveEventListUseCase(repository),
    refreshEventsUseCase = RefreshEventsUseCase(repository),
    dispatchers = dispatchers,
  )

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
    var refreshEventsCallCount: Int = 0
      private set

    override fun getEvents(): Flow<List<EventPreview>> = eventsFlow

    override fun getEventDetails(eventId: String): Flow<EventDetails?> = flowOf(null)

    override suspend fun addToFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEvents(): Result<Unit> {
      refreshEventsCallCount += 1
      eventsFlow.value = refreshedEvents
      return Result.success(Unit)
    }

    override suspend fun refreshEventDetails(eventId: String): Result<Unit> = Result.success(Unit)
  }

  private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
