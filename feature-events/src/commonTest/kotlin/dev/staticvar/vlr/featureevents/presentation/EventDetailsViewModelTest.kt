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
import dev.staticvar.vlr.featureevents.usecase.ObserveEventDetailsUseCase
import dev.staticvar.vlr.featureevents.usecase.RefreshEventDetailsUseCase
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

  private fun createViewModel(repository: FakeEventRepository): EventDetailsViewModel = EventDetailsViewModel(
    observeEventDetailsUseCase = ObserveEventDetailsUseCase(repository),
    refreshEventDetailsUseCase = RefreshEventDetailsUseCase(repository),
    dispatchers = dispatchers,
  )

  private class FakeEventRepository(details: EventDetails? = null) : EventRepository {
    val refreshDetailRequests: MutableList<String> = mutableListOf()
    private val detailsByEventId: MutableMap<String, MutableStateFlow<EventDetails?>> = mutableMapOf()

    init {
      detailsByEventId["event-1"] = MutableStateFlow(details)
    }

    override fun getEvents(): Flow<List<EventPreview>> = flowOf(emptyList())

    override fun getEventDetails(eventId: String): Flow<EventDetails?> = detailsByEventId.getOrPut(eventId) {
      MutableStateFlow(
        EventDetails(
          id = eventId,
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
    }

    override suspend fun addToFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEvents(): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEventDetails(eventId: String): Result<Unit> {
      refreshDetailRequests += eventId
      return Result.success(Unit)
    }
  }

  private class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val default: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
  }
}
