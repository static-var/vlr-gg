/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.usecase

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.usecase.InitialFavoriteProfilesRefresh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class EventsUseCasesTest {
  @Test
  fun observeEventListReturnsRepositoryData() {
    runTest {
      val expected = listOf(eventPreview(id = "event-1"))
      val repository = FakeEventRepository(events = expected)

      val actual = ObserveEventListUseCase(repository)().first()

      assertEquals(expected, actual)
    }
  }

  @Test
  fun refreshEventsWaitsForProfilesAndStillFetchesAfterProfileFailure() {
    runTest {
      val calls = mutableListOf<String>()
      val expected = IllegalStateException("profile refresh failed")
      val repository = FakeEventRepository(events = emptyList()) {
        calls += "events"
        Result.success(Unit)
      }
      val profiles = InitialFavoriteProfilesRefresh {
        calls += "profiles"
        Result.failure(expected)
      }

      val result = RefreshEventsUseCase(repository, profiles)()

      assertSame(expected, result.exceptionOrNull())
      assertEquals(listOf("profiles", "events"), calls)
      assertEquals(1, repository.refreshEventsCalls)
    }
  }

  @Test
  fun observeEventDetailsRequestsRepositoryById() {
    runTest {
      val expected = eventDetails(id = "event-9")
      val repository = FakeEventRepository(details = expected)

      val actual = ObserveEventDetailsUseCase(repository)("event-9").first()

      assertEquals("event-9", repository.observedEventId)
      assertEquals(expected, actual)
    }
  }

  @Test
  fun refreshEventDetailsDelegatesRequestedId() {
    runTest {
      val repository = FakeEventRepository(events = emptyList())

      val result = RefreshEventDetailsUseCase(repository)("event-3")

      assertEquals(true, result.isSuccess)
      assertEquals("event-3", repository.refreshedEventId)
    }
  }

  private class FakeEventRepository(
    private val events: List<EventPreview> = emptyList(),
    private val details: EventDetails? = null,
    private val onRefreshEvents: suspend () -> Result<Unit> = { Result.success(Unit) },
  ) : EventRepository {
    var refreshEventsCalls: Int = 0
      private set
    var observedEventId: String? = null
      private set
    var refreshedEventId: String? = null
      private set

    override fun getEvents(): Flow<List<EventPreview>> = flowOf(events)

    override fun getEventDetails(eventId: String): Flow<EventDetails?> {
      observedEventId = eventId
      return flowOf(details)
    }

    override suspend fun addToFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(eventId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshEvents(): Result<Unit> {
      refreshEventsCalls += 1
      return onRefreshEvents()
    }

    override suspend fun refreshEventDetails(eventId: String): Result<Unit> {
      refreshedEventId = eventId
      return Result.success(Unit)
    }
  }
}

private fun eventPreview(id: String): EventPreview = EventPreview(
  id = id,
  title = "Champions",
  status = EventStatus.ONGOING,
  prize = "$" + "100k",
  dates = "Mar 1 - Mar 7",
  region = "Global",
  logoUrl = "",
)

private fun eventDetails(id: String): EventDetails = EventDetails(
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
