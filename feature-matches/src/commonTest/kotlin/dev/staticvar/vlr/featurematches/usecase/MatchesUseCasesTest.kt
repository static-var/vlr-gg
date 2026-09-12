/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.usecase

import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.MatchRepository
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
class MatchesUseCasesTest {
  @Test
  fun observeMatchListReturnsRepositoryData() {
    runTest {
      val expected = listOf(matchPreview(id = "match-1"))
      val repository = FakeMatchRepository(matches = expected)

      val actual = ObserveMatchListUseCase(repository)().first()

      assertEquals(expected, actual)
    }
  }

  @Test
  fun refreshMatchesWaitsForProfilesAndStillFetchesAfterProfileFailure() {
    runTest {
      val calls = mutableListOf<String>()
      val expected = IllegalStateException("profile refresh failed")
      val repository = FakeMatchRepository(matches = emptyList()) {
        calls += "matches"
        Result.success(Unit)
      }
      val profiles = InitialFavoriteProfilesRefresh {
        calls += "profiles"
        Result.failure(expected)
      }

      val result = RefreshMatchesUseCase(repository, profiles)()

      assertSame(expected, result.exceptionOrNull())
      assertEquals(listOf("profiles", "matches"), calls)
      assertEquals(1, repository.refreshMatchesCalls)
    }
  }

  @Test
  fun observeMatchDetailsRequestsRepositoryById() {
    runTest {
      val expected = matchDetails(id = "match-7")
      val repository = FakeMatchRepository(details = expected)

      val actual = ObserveMatchDetailsUseCase(repository)("match-7").first()

      assertEquals("match-7", repository.observedMatchId)
      assertEquals(expected, actual)
    }
  }

  @Test
  fun refreshMatchDetailsDelegatesRequestedId() {
    runTest {
      val repository = FakeMatchRepository(matches = emptyList())

      val result = RefreshMatchDetailsUseCase(repository)("match-9")

      assertEquals(true, result.isSuccess)
      assertEquals("match-9", repository.refreshedMatchId)
    }
  }

  private class FakeMatchRepository(
    private val matches: List<MatchPreview> = emptyList(),
    private val details: MatchDetails? = null,
    private val onRefreshMatches: suspend () -> Result<Unit> = { Result.success(Unit) },
  ) : MatchRepository {
    var refreshMatchesCalls: Int = 0
      private set
    var observedMatchId: String? = null
      private set
    var refreshedMatchId: String? = null
      private set

    override fun getMatches(): Flow<List<MatchPreview>> = flowOf(matches)

    override fun getMatchDetails(matchId: String): Flow<MatchDetails?> {
      observedMatchId = matchId
      return flowOf(details)
    }

    override suspend fun addToFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun removeFromFavorites(matchId: String): Result<Unit> = Result.success(Unit)

    override suspend fun refreshMatches(): Result<Unit> {
      refreshMatchesCalls += 1
      return onRefreshMatches()
    }

    override suspend fun refreshMatchDetails(matchId: String): Result<Unit> {
      refreshedMatchId = matchId
      return Result.success(Unit)
    }
  }
}

private fun matchPreview(id: String): MatchPreview = MatchPreview(
  id = id,
  event = "Masters",
  series = "Bo3",
  status = MatchStatus.LIVE,
  team1 = teamPreview(name = "Alpha"),
  team2 = teamPreview(name = "Bravo"),
  time = "12:00",
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

private fun matchDetails(id: String): MatchDetails = MatchDetails(
  id = id,
  event = EventInfo(
    id = "event-1",
    name = "Masters",
    series = "Bo3",
    stage = "Playoffs",
    img = "",
    date = "Today",
    patch = null,
    status = "LIVE",
  ),
  head2head = emptyList(),
  note = "",
  score = "0:0",
  teams = emptyList(),
  bans = emptyList(),
  videos = MatchVideos(streams = emptyList(), vods = emptyList()),
  matchData = emptyList(),
  mapCount = 0,
)
