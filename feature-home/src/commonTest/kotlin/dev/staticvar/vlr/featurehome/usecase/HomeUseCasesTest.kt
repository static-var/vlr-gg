/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.usecase

import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.featurehome.presentation.initialHomeMatchPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class HomeUseCasesTest {
  @Test
  fun homeShowsRecentMatchesAndCurrentEventsChronologicallyInBothSections() = runTest {
    val directFavorites = DirectFavoriteSnapshot(
      teams = listOf(DirectFavorite.Team("team-1", "Alpha", "")),
      players = listOf(DirectFavorite.Player("player-1", "Player", "")),
      matches = listOf("live", "done", "early", "unknown", "missing").map {
        DirectFavorite.Match(it, it, "")
      },
      events = listOf("ongoing", "done-event", "early-event", "paused", "unknown-event", "missing-event").map {
        DirectFavorite.Event(it, it, "")
      },
    )
    val matches = listOf(
      match("live", MatchStatus.LIVE, "2026-09-12T12:00:00Z", "ongoing"),
      match("done", MatchStatus.COMPLETED, "2026-09-01T12:00:00Z", "active-from-done"),
      match("early", MatchStatus.UPCOMING, "2026-09-11T12:00:00Z", "early-event"),
      match("unknown", MatchStatus.UNKNOWN, null, "unknown-event"),
      match("indirect", MatchStatus.UPCOMING, "2026-09-13T12:00:00Z", "related-event"),
      match("indirect", MatchStatus.UPCOMING, "2026-09-13T12:00:00Z", "related-event"),
      match("no-date", MatchStatus.UPCOMING, null, "   "),
      match("unrelated", MatchStatus.LIVE, null, "unrelated-event", personalized = false),
    )
    val events = listOf(
      event("ongoing", EventStatus.ONGOING, "Aug 10—Sep 12"),
      event("active-from-done", EventStatus.ONGOING, "Jun 5—Sep 20"),
      event("done-event", EventStatus.COMPLETED, "Jan 1—7"),
      event("early-event", EventStatus.UPCOMING, "Jun 17—Sep 18"),
      event("related-event", EventStatus.UPCOMING, "Sep 24—Oct 18"),
      event("related-event", EventStatus.UPCOMING, "Sep 24—Oct 18"),
      event("paused", EventStatus.PAUSED, "May 1—2"),
      event("unknown-event", EventStatus.UNKNOWN, "TBD"),
      event("unrelated-event", EventStatus.ONGOING, "Apr 1—7"),
      event("   ", EventStatus.UPCOMING, "Jan 1—2"),
    )

    val feed = ObserveHomeFeedUseCase(
      FakeFavoritesRepository(directFavorites), FakeMatchRepository(matches), FakeEventRepository(events),
      dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
      clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-09-12T12:00:00Z")
      },
    )().first()

    assertTrue(feed.hasDirectFavorites)
    assertEquals(listOf("early", "live", "indirect", "no-date"), feed.personalizedMatches.map { it.id })
    assertEquals(
      listOf("active-from-done", "early-event", "ongoing", "related-event"),
      feed.personalizedEvents.map { it.id },
    )
    assertEquals(listOf("early", "live"), feed.directFavorites.matches.map { it.id })
    assertEquals(listOf("early-event", "ongoing"), feed.directFavorites.events.map { it.id })
    assertEquals(directFavorites.teams, feed.directFavorites.teams)
    assertEquals(directFavorites.players, feed.directFavorites.players)
    assertEquals(5, directFavorites.matches.size)
    assertEquals(6, directFavorites.events.size)
  }

  @Test
  fun homeFeedAssemblyRunsOnDefaultDispatcher() = runTest {
    val defaultDispatcher = TrackingDispatcher(StandardTestDispatcher(testScheduler))
    var clockInvocations = 0
    var clockObservedDefaultDispatcher = false
    val useCase = ObserveHomeFeedUseCase(
      favoritesRepository = FakeFavoritesRepository(DirectFavoriteSnapshot()),
      matchRepository = FakeMatchRepository(emptyList()),
      eventRepository = FakeEventRepository(emptyList()),
      dispatchers = TestDispatcherProvider(defaultDispatcher),
      clock = object : Clock {
        override fun now(): Instant {
          clockInvocations++
          clockObservedDefaultDispatcher = defaultDispatcher.isRunning
          return Instant.parse("2026-09-12T12:00:00Z")
        }
      },
    )

    useCase().first()

    assertEquals(1, clockInvocations)
    assertTrue(clockObservedDefaultDispatcher)
  }

  @Test
  fun matchWindowIncludesBoundaryAndFutureWhileRetainingOlderLiveMatches() {
    val matches = listOf(
      match("future", MatchStatus.UPCOMING, "2026-09-21T12:00:00Z", "event"),
      match("live", MatchStatus.LIVE, "2026-09-20T10:00:00Z", "event"),
      match("before-window", MatchStatus.COMPLETED, "2026-09-19T11:59:59.999Z", "event"),
      match("boundary", MatchStatus.COMPLETED, "2026-09-19T12:00:00Z", "event"),
      match("old-live", MatchStatus.LIVE, "2026-09-18T12:00:00Z", "event"),
      match("stale-upcoming", MatchStatus.UPCOMING, "2026-09-18T12:00:00Z", "event"),
      match("undated-upcoming", MatchStatus.UPCOMING, "TBD", "event"),
      match("undated-completed", MatchStatus.COMPLETED, null, "event"),
    )
    val feed = buildHomeFeed(
      DirectFavoriteSnapshot(matches = matches.map { DirectFavorite.Match(it.id, it.id, "") }),
      matches,
      emptyList(),
      now = Instant.parse("2026-09-20T12:00:00Z"),
    )

    val expected = listOf("old-live", "boundary", "live", "future", "undated-upcoming")
    assertEquals(expected, feed.personalizedMatches.map { it.id })
    assertEquals(expected, feed.directFavorites.matches.map { it.id })
  }

  @Test
  fun liveSelectionKeepsCompletedMatchBeforeItAndSelectsFirstLiveMatch() {
    val feed = buildHomeFeed(
      DirectFavoriteSnapshot(teams = listOf(DirectFavorite.Team("team-1", "Alpha", ""))),
      listOf(
        match("later-live", MatchStatus.LIVE, "2026-09-20T11:00:00Z", "event"),
        match("future", MatchStatus.UPCOMING, "2026-09-21T12:00:00Z", "event"),
        match("live", MatchStatus.LIVE, "2026-09-20T10:00:00Z", "event"),
        match("completed", MatchStatus.COMPLETED, "2026-09-19T14:00:00Z", "event"),
      ),
      emptyList(),
      now = Instant.parse("2026-09-20T12:00:00Z"),
    )

    assertEquals(listOf("completed", "live", "later-live", "future"), feed.personalizedMatches.map { it.id })
    assertEquals(1, initialHomeMatchPage(feed.personalizedMatches))
    assertEquals(0, initialHomeMatchPage(feed.personalizedMatches.filter { it.status != MatchStatus.LIVE }))
    assertEquals(0, initialHomeMatchPage(emptyList()))
  }

  @Test
  fun playerTransferMovesPersonalizedMatchesAndEventsWhileKeepingPlayerFavorite() {
    val favorites = DirectFavoriteSnapshot(
      players = listOf(DirectFavorite.Player("player-1", "Player", "")),
    )
    val oldMatch = match("old-team", MatchStatus.UPCOMING, null, "old-event", personalized = false)
    val newMatch = match("new-team", MatchStatus.UPCOMING, null, "new-event", personalized = false)
    val playerReason = MatchFavoriteReason(MatchFavoriteSource.PLAYER, "player-1", "Player")
    val events = listOf(
      event("old-event", EventStatus.UPCOMING, "Sep 14—15"),
      event("new-event", EventStatus.UPCOMING, "Sep 16—17"),
    )

    val before = buildHomeFeed(
      favorites,
      listOf(oldMatch.copy(isFavorite = true, favoriteReasons = listOf(playerReason)), newMatch),
      events,
    )
    val after = buildHomeFeed(
      favorites,
      listOf(oldMatch, newMatch.copy(isFavorite = true, favoriteReasons = listOf(playerReason))),
      events,
    )

    assertEquals(listOf("old-team"), before.personalizedMatches.map { it.id })
    assertEquals(listOf("old-event"), before.personalizedEvents.map { it.id })
    assertEquals(listOf("new-team"), after.personalizedMatches.map { it.id })
    assertEquals(listOf("new-event"), after.personalizedEvents.map { it.id })
    assertEquals(favorites.players, after.directFavorites.players)
    assertTrue(after.directFavorites.teams.isEmpty())
  }

  @Test
  fun completedFavoritesKeepHomeAvailableWithoutDisplayingFinishedItems() {
    val feed = buildHomeFeed(
      DirectFavoriteSnapshot(
        matches = listOf(DirectFavorite.Match("done", "Done", "")),
        events = listOf(DirectFavorite.Event("done-event", "Done event", "")),
      ),
      listOf(match("done", MatchStatus.COMPLETED, "2026-09-01T12:00:00Z", "done-event")),
      listOf(event("done-event", EventStatus.COMPLETED, "Sep 1—2")),
      now = Instant.parse("2026-09-20T12:00:00Z"),
    )
    assertTrue(feed.hasDirectFavorites)
    assertTrue(feed.personalizedMatches.isEmpty())
    assertTrue(feed.personalizedEvents.isEmpty())
    assertTrue(feed.directFavorites.matches.isEmpty())
    assertTrue(feed.directFavorites.events.isEmpty())
  }

  @Test
  fun refreshKeepsSuccessfulCacheUpdateWhenSiblingThrows() = runTest {
    val matchStarted = CompletableDeferred<Unit>()
    val eventStarted = CompletableDeferred<Unit>()
    var matchCompleted = false
    val expected = IllegalStateException("event refresh failed")
    val useCase = RefreshHomeUseCase(
      matchRepository = FakeMatchRepository(emptyList()) {
        matchStarted.complete(Unit)
        eventStarted.await()
        matchCompleted = true
        Result.success(Unit)
      },
      eventRepository = FakeEventRepository(emptyList()) {
        eventStarted.complete(Unit)
        matchStarted.await()
        throw expected
      },
    )

    val completed = useCase()

    assertTrue(matchCompleted)
    assertSame(expected, completed.exceptionOrNull())
  }

  @Test
  fun refreshFetchesBothListsWhenMatchRefreshFails() = runTest {
    val expected = IllegalStateException("match refresh failed")
    var matchesRefreshed = false
    var eventsRefreshed = false
    val useCase = RefreshHomeUseCase(
      matchRepository = FakeMatchRepository(emptyList()) {
        matchesRefreshed = true
        Result.failure(expected)
      },
      eventRepository = FakeEventRepository(emptyList()) {
        eventsRefreshed = true
        Result.success(Unit)
      },
    )

    val result = useCase()

    assertTrue(matchesRefreshed)
    assertTrue(eventsRefreshed)
    assertSame(expected, result.exceptionOrNull())
  }

  @Test
  fun refreshPropagatesWrappedCancellationAndCancelsSibling() = runTest {
    val matchStarted = CompletableDeferred<Unit>()
    var matchCancelled = false
    val useCase = RefreshHomeUseCase(
      matchRepository = FakeMatchRepository(emptyList()) {
        matchStarted.complete(Unit)
        try {
          awaitCancellation()
        } finally {
          matchCancelled = true
        }
      },
      eventRepository = FakeEventRepository(emptyList()) {
        matchStarted.await()
        Result.failure(CancellationException("cancel home refresh"))
      },
    )

    assertFailsWith<CancellationException> { useCase() }
    assertTrue(matchCancelled)
  }
}

private class TestDispatcherProvider(
  override val default: CoroutineDispatcher,
) : DispatcherProvider {
  override val io: CoroutineDispatcher = default
  override val main: CoroutineDispatcher = default
}

private class TrackingDispatcher(
  private val delegate: CoroutineDispatcher,
) : CoroutineDispatcher() {
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

private class FakeFavoritesRepository(
  private val directFavorites: DirectFavoriteSnapshot,
) : FavoritesRepository {
  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = flowOf(directFavorites)

  override fun observeTeamIds(): Flow<Set<String>> = flowOf(directFavorites.teams.mapTo(mutableSetOf()) { it.id })

  override fun observePlayerIds(): Flow<Set<String>> = flowOf(directFavorites.players.mapTo(mutableSetOf()) { it.id })
}

private class FakeMatchRepository(
  private val matches: List<MatchPreview>,
  private val refresh: suspend () -> Result<Unit> = { Result.success(Unit) },
) : MatchRepository {
  override fun getMatches(): Flow<List<MatchPreview>> = flowOf(matches)

  override fun getMatchDetails(matchId: String): Flow<MatchDetails?> = flowOf(null)

  override suspend fun addToFavorites(matchId: String): Result<Unit> = Result.success(Unit)

  override suspend fun removeFromFavorites(matchId: String): Result<Unit> = Result.success(Unit)

  override suspend fun refreshMatches(): Result<Unit> = refresh()

  override suspend fun refreshMatchDetails(matchId: String): Result<Unit> = Result.success(Unit)
}

private class FakeEventRepository(
  private val events: List<EventPreview>,
  private val refresh: suspend () -> Result<Unit> = { Result.success(Unit) },
) : EventRepository {
  override fun getEvents(): Flow<List<EventPreview>> = flowOf(events)

  override fun getEventDetails(eventId: String): Flow<EventDetails?> = flowOf(null)

  override suspend fun addToFavorites(eventId: String): Result<Unit> = Result.success(Unit)

  override suspend fun removeFromFavorites(eventId: String): Result<Unit> = Result.success(Unit)

  override suspend fun refreshEvents(): Result<Unit> = refresh()

  override suspend fun refreshEventDetails(eventId: String): Result<Unit> = Result.success(Unit)
}

private fun match(
  id: String,
  status: MatchStatus,
  time: String?,
  eventId: String,
  personalized: Boolean = true,
): MatchPreview = MatchPreview(
  id = id,
  event = "Event $eventId",
  series = "Bo3",
  status = status,
  team1 = team("Alpha"),
  team2 = team("Bravo"),
  time = time,
  eventId = eventId,
  isFavorite = personalized,
  favoriteReasons = if (personalized) {
    listOf(MatchFavoriteReason(MatchFavoriteSource.TEAM, "team-1", "Alpha"))
  } else {
    emptyList()
  },
)

private fun team(name: String): TeamPreview = TeamPreview(
  id = name.lowercase(),
  name = name,
  region = "Global",
  img = "",
  score = null,
  isWinner = null,
)

private fun event(id: String, status: EventStatus, dates: String): EventPreview = EventPreview(
  id = id,
  title = "Event $id",
  status = status,
  prize = "$" + "100,000",
  dates = dates,
  region = "Global",
  logoUrl = "",
)
