/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurehome.usecase

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HomeUseCasesTest {
  @Test
  fun homeShowsOnlyCurrentMatchesAndEventsChronologicallyInBothSections() = runTest {
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
  fun completedFavoritesKeepHomeAvailableWithoutDisplayingFinishedItems() {
    val feed = buildHomeFeed(
      DirectFavoriteSnapshot(
        matches = listOf(DirectFavorite.Match("done", "Done", "")),
        events = listOf(DirectFavorite.Event("done-event", "Done event", "")),
      ),
      listOf(match("done", MatchStatus.COMPLETED, "2026-09-01T12:00:00Z", "done-event")),
      listOf(event("done-event", EventStatus.COMPLETED, "Sep 1—2")),
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

    val result = useCase()

    assertTrue(matchCompleted)
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
