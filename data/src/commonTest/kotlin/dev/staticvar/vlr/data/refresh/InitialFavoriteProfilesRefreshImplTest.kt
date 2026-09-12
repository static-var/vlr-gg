/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.refresh

import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InitialFavoriteProfilesRefreshImplTest {
  @Test
  fun refreshesEveryFavoriteWithBoundedConcurrencyThenCachesSuccess() = runTest {
    val release = CompletableDeferred<Unit>()
    val tracker = RefreshTracker(release)
    val refresh = createRefresh(
      favorites = favorites(playerCount = 5, teamCount = 2),
      playerRefresh = tracker::refresh,
      teamRefresh = tracker::refresh,
    )

    val result = async { refresh.awaitInitialRefresh() }
    runCurrent()

    assertEquals(4, tracker.active)
    release.complete(Unit)
    assertTrue(result.await().isSuccess)
    assertEquals((1..5).map { "player-$it" }.toSet(), tracker.playerIds)
    assertEquals((1..2).map { "team-$it" }.toSet(), tracker.teamIds)
    assertEquals(4, tracker.peakActive)

    assertTrue(refresh.awaitInitialRefresh().isSuccess)
    assertEquals(7, tracker.calls)
  }

  @Test
  fun concurrentCallersShareFailureAndNextCallRetries() = runTest {
    val firstStarted = CompletableDeferred<Unit>()
    val finishFirst = CompletableDeferred<Unit>()
    val expected = IllegalStateException("profile refresh failed")
    var calls = 0
    val refresh = createRefresh(
      favorites = favorites(playerCount = 1),
      playerRefresh = {
        calls += 1
        if (calls == 1) {
          firstStarted.complete(Unit)
          finishFirst.await()
          Result.failure(expected)
        } else {
          Result.success(Unit)
        }
      },
    )

    val first = async { refresh.awaitInitialRefresh() }
    firstStarted.await()
    val second = async { refresh.awaitInitialRefresh() }
    runCurrent()

    assertEquals(1, calls)
    finishFirst.complete(Unit)
    assertSame(expected, first.await().exceptionOrNull())
    assertSame(expected, second.await().exceptionOrNull())

    assertTrue(refresh.awaitInitialRefresh().isSuccess)
    assertEquals(2, calls)
  }

  @Test
  fun survivingWaiterRetriesAfterLeaderCancellation() = runTest {
    val firstStarted = CompletableDeferred<Unit>()
    var calls = 0
    val refresh = createRefresh(
      favorites = favorites(playerCount = 1),
      playerRefresh = {
        calls += 1
        if (calls == 1) {
          firstStarted.complete(Unit)
          awaitCancellation()
        }
        Result.success(Unit)
      },
    )

    val leader = async { refresh.awaitInitialRefresh() }
    firstStarted.await()
    val waiter = async { refresh.awaitInitialRefresh() }
    runCurrent()

    leader.cancel(CancellationException("cancel initial refresh"))
    assertFailsWith<CancellationException> { leader.await() }
    assertTrue(waiter.await().isSuccess)
    assertEquals(2, calls)
  }
}

private fun createRefresh(
  favorites: DirectFavoriteSnapshot,
  playerRefresh: suspend (String) -> Result<Unit> = { Result.success(Unit) },
  teamRefresh: suspend (String) -> Result<Unit> = { Result.success(Unit) },
): InitialFavoriteProfilesRefreshImpl = InitialFavoriteProfilesRefreshImpl(
  favoritesRepository = FakeFavoritesRepository(favorites),
  playerRepository = FakePlayerRepository(playerRefresh),
  teamRepository = FakeTeamRepository(teamRefresh),
)

private fun favorites(playerCount: Int = 0, teamCount: Int = 0): DirectFavoriteSnapshot = DirectFavoriteSnapshot(
  players = (1..playerCount).map { id -> DirectFavorite.Player("player-$id", "Player $id", "") },
  teams = (1..teamCount).map { id -> DirectFavorite.Team("team-$id", "Team $id", "") },
)

private class RefreshTracker(private val release: CompletableDeferred<Unit>) {
  var active: Int = 0
    private set
  var peakActive: Int = 0
    private set
  var calls: Int = 0
    private set
  val playerIds: MutableSet<String> = mutableSetOf()
  val teamIds: MutableSet<String> = mutableSetOf()

  suspend fun refresh(id: String): Result<Unit> {
    calls += 1
    if (id.startsWith("player-")) playerIds += id else teamIds += id
    active += 1
    peakActive = maxOf(peakActive, active)
    return try {
      release.await()
      Result.success(Unit)
    } finally {
      active -= 1
    }
  }
}

private class FakeFavoritesRepository(private val favorites: DirectFavoriteSnapshot) : FavoritesRepository {
  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = flowOf(favorites)
  override fun observeTeamIds(): Flow<Set<String>> = flowOf(favorites.teams.map { it.id }.toSet())
  override fun observePlayerIds(): Flow<Set<String>> = flowOf(favorites.players.map { it.id }.toSet())
}

private class FakePlayerRepository(
  private val refresh: suspend (String) -> Result<Unit>,
) : PlayerRepository {
  override fun getPlayerInTeam(teamId: String): Flow<List<PlayerInfo?>> = flowOf(emptyList())
  override fun getPlayerDetails(playerId: String): Flow<PlayerInfo?> = flowOf(null)
  override suspend fun addToFavorites(playerId: String): Result<Unit> = Result.success(Unit)
  override suspend fun removeFromFavorites(playerId: String): Result<Unit> = Result.success(Unit)
  override suspend fun refreshPlayerDetails(playerId: String): Result<Unit> = refresh(playerId)
}

private class FakeTeamRepository(
  private val refresh: suspend (String) -> Result<Unit>,
) : TeamRepository {
  override fun getTeams(): Flow<List<TeamInfo>> = flowOf(emptyList())
  override fun getTeamDetails(teamId: String): Flow<TeamInfo?> = flowOf(null)
  override fun getTeamsByRegion(region: String): Flow<List<TeamInfo>> = flowOf(emptyList())
  override suspend fun addToFavorites(teamId: String): Result<Unit> = Result.success(Unit)
  override suspend fun removeFromFavorites(teamId: String): Result<Unit> = Result.success(Unit)
  override suspend fun refreshTeamDetails(teamId: String): Result<Unit> = refresh(teamId)
}
