/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import com.russhwolf.settings.MapSettings
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.core.identity.UserIdentityRepository
import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.FavoriteMatchFeed
import dev.staticvar.vlr.domain.repository.FavoriteMatchesRetryLaterException
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.remotesource.match.FavoriteMatchesDataSource
import dev.staticvar.vlr.remotesource.match.FavoriteMatchesUnavailableException
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.TeamDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import dev.staticvar.vlr.remotesource.common.MatchStatus as RemoteMatchStatus

class FavoriteMatchesRepositoryImplTest {
  private val identity = UserIdentityRepository(MapSettings())
  private val favorites = FakeFavoritesRepository(teamFavorite)

  @Test
  fun `successful fetch maps every DTO and publishes the home feed`() = runTest {
    val source = FakeFavoriteMatchesSource { Result.success(listOf(completedDto, upcomingDto)) }
    val repository = repository(source)

    val result = repository.fetch(includeResults = true)

    assertEquals(listOf(completedPreview, upcomingPreview), result.getOrThrow())
    assertEquals(FavoriteMatchFeed(teamFavorite, listOf(completedPreview, upcomingPreview)), repository.homeMatches.value)
    assertEquals(listOf(identity.id.value.toString() to true), source.requests)
  }

  @Test
  fun `request and DTO traversal run on io instead of the caller dispatcher`() = runTest {
    val main = TrackingDispatcher(StandardTestDispatcher(testScheduler))
    val io = TrackingDispatcher(StandardTestDispatcher(testScheduler))
    fun running() = when {
      io.isRunning -> "io"
      main.isRunning -> "main"
      else -> "other"
    }
    val seen = mutableSetOf<String>()
    val source = FakeFavoriteMatchesSource {
      seen += "request on ${running()}"
      Result.success(TrackedList(listOf(completedDto)) { seen += "DTO read on ${running()}" })
    }
    val repository = repository(source, TestDispatcherProvider(StandardTestDispatcher(testScheduler), io = io, main = main))

    val result = withContext(main) { repository.fetch(includeResults = true) }

    assertEquals(listOf(completedPreview), result.getOrThrow())
    assertEquals(setOf("request on io", "DTO read on io"), seen)
  }

  @Test
  fun `fetch without results never replaces the home feed`() = runTest {
    val source = FakeFavoriteMatchesSource { includeResults ->
      Result.success(if (includeResults) listOf(completedDto) else listOf(upcomingDto))
    }
    val repository = repository(source)

    assertEquals(listOf(upcomingPreview), repository.fetch(includeResults = false).getOrThrow())
    assertNull(repository.homeMatches.value)

    repository.fetch(includeResults = true)
    assertEquals(listOf(upcomingPreview), repository.fetch(includeResults = false).getOrThrow())

    assertEquals(FavoriteMatchFeed(teamFavorite, listOf(completedPreview)), repository.homeMatches.value)
    assertEquals(listOf(false, true, false), source.requests.map { it.second })
  }

  @Test
  fun `home feed keeps the favorites captured before the request`() = runTest {
    val response = CompletableDeferred<Result<List<MatchPreviewDto>>>()
    val repository = repository(FakeFavoriteMatchesSource { response.await() })

    val fetch = async { repository.fetch(includeResults = true) }
    runCurrent()
    favorites.snapshots.value = DirectFavoriteSnapshot()
    response.complete(Result.success(listOf(completedDto)))
    fetch.await()

    assertEquals(FavoriteMatchFeed(teamFavorite, listOf(completedPreview)), repository.homeMatches.value)
  }

  @Test
  fun `failures keep the cached feed and translate unavailable responses to retry later`() = runTest {
    val unexpected = IllegalStateException("broken")
    val responses = ArrayDeque(
      listOf(
        Result.success(listOf(completedDto)),
        Result.failure(FavoriteMatchesUnavailableException(404)),
        Result.failure(FavoriteMatchesUnavailableException(503)),
        Result.failure(unexpected),
      ),
    )
    val repository = repository(FakeFavoriteMatchesSource { responses.removeFirst() })
    val feed = FavoriteMatchFeed(teamFavorite, listOf(completedPreview))
    repository.fetch(includeResults = true)

    assertEquals(404, assertIs<FavoriteMatchesRetryLaterException>(repository.fetch(true).exceptionOrNull()).statusCode)
    assertEquals(feed, repository.homeMatches.value)
    assertEquals(503, assertIs<FavoriteMatchesRetryLaterException>(repository.fetch(true).exceptionOrNull()).statusCode)
    assertEquals(feed, repository.homeMatches.value)
    assertSame(unexpected, repository.fetch(true).exceptionOrNull())
    assertEquals(feed, repository.homeMatches.value)
  }

  @Test
  fun `cancelling the caller cancels the request without publishing`() = runTest {
    var requestCancelled = false
    val repository = repository(
      FakeFavoriteMatchesSource {
        try {
          awaitCancellation()
        } finally {
          requestCancelled = true
        }
      },
    )

    val fetch = launch { repository.fetch(includeResults = true) }
    runCurrent()
    fetch.cancelAndJoin()

    assertTrue(requestCancelled)
    assertNull(repository.homeMatches.value)
  }

  private fun TestScope.repository(
    source: FavoriteMatchesDataSource,
    dispatchers: DispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
  ) = FavoriteMatchesRepositoryImpl(source = source, identity = identity, favorites = favorites, dispatchers = dispatchers)

  private companion object {
    val teamFavorite = DirectFavoriteSnapshot(teams = listOf(DirectFavorite.Team(id = "t1", title = "Fnatic", imageUrl = "")))

    val completedDto = MatchPreviewDto(
      id = "m1",
      event = "Masters",
      series = "Grand Final",
      status = RemoteMatchStatus.COMPLETED,
      team1 = TeamDto(id = "t1", name = "Fnatic", img = "fnatic.png", score = 3),
      team2 = TeamDto(id = "t2", name = "Sentinels", img = "sentinels.png", score = 1),
      time = "2026-10-17T10:00:00Z",
      eventId = "e1",
    )
    val completedPreview = MatchPreview(
      id = "m1",
      event = "Masters",
      series = "Grand Final",
      status = MatchStatus.COMPLETED,
      team1 = TeamPreview(id = "t1", name = "Fnatic", region = "", img = "fnatic.png", score = 3, isWinner = true),
      team2 = TeamPreview(id = "t2", name = "Sentinels", region = "", img = "sentinels.png", score = 1, isWinner = false),
      time = "2026-10-17T10:00:00Z",
      eventId = "e1",
    )

    val upcomingDto = MatchPreviewDto(
      id = "m2",
      status = RemoteMatchStatus.UPCOMING,
      team1 = TeamDto(name = "Fnatic"),
      team2 = TeamDto(name = "TBD"),
    )
    val upcomingPreview = MatchPreview(
      id = "m2",
      event = "",
      series = "",
      status = MatchStatus.UPCOMING,
      team1 = TeamPreview(id = null, name = "Fnatic", region = "", img = "", score = null, isWinner = null),
      team2 = TeamPreview(id = null, name = "TBD", region = "", img = "", score = null, isWinner = null),
      time = null,
      eventId = "",
    )
  }
}

private class FakeFavoriteMatchesSource(
  private val respond: suspend (includeResults: Boolean) -> Result<List<MatchPreviewDto>>,
) : FavoriteMatchesDataSource {
  val requests = mutableListOf<Pair<String, Boolean>>()

  override suspend fun list(clientId: String, includeResults: Boolean): Result<List<MatchPreviewDto>> {
    requests += clientId to includeResults
    return respond(includeResults)
  }
}

private class FakeFavoritesRepository(initial: DirectFavoriteSnapshot) : FavoritesRepository {
  val snapshots = MutableStateFlow(initial)

  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = snapshots

  override fun observeTeamIds(): Flow<Set<String>> = emptyFlow()

  override fun observePlayerIds(): Flow<Set<String>> = emptyFlow()
}

private class TrackedList<T>(private val items: List<T>, private val onRead: () -> Unit) : AbstractList<T>() {
  override val size: Int
    get() = items.size

  override fun get(index: Int): T {
    onRead()
    return items[index]
  }
}

private class TestDispatcherProvider(
  override val default: CoroutineDispatcher,
  override val io: CoroutineDispatcher = default,
  override val main: CoroutineDispatcher = default,
) : DispatcherProvider

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
