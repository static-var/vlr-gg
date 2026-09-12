/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.domain.usecase

import dev.staticvar.vlr.domain.model.DirectFavorite
import dev.staticvar.vlr.domain.model.DirectFavoriteSnapshot
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventInfo
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.FavoriteScheduledMatch
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus
import dev.staticvar.vlr.domain.model.MatchVideos
import dev.staticvar.vlr.domain.model.PlayerInfo
import dev.staticvar.vlr.domain.model.PlayerTeam
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.domain.model.TeamPreview
import dev.staticvar.vlr.domain.repository.EventRepository
import dev.staticvar.vlr.domain.repository.FavoriteScheduleRepository
import dev.staticvar.vlr.domain.repository.FavoritesRepository
import dev.staticvar.vlr.domain.repository.MatchRepository
import dev.staticvar.vlr.domain.repository.PlayerRepository
import dev.staticvar.vlr.domain.repository.TeamRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RefreshFavoriteMatchesTest {
  @Test
  fun noFavoritesDoNotStartNetworkRefreshes() = runTest {
    val fixture = Fixture()

    fixture.useCase()

    assertEquals(0, fixture.matches.overviewRefreshes)
    assertEquals(emptyList(), fixture.matches.detailRefreshes)
    assertEquals(emptyList(), fixture.players.refreshes)
    assertEquals(emptyList(), fixture.teams.refreshes)
    assertEquals(emptyList(), fixture.events.refreshes)
  }

  @Test
  fun directFavoritesAndStoredSchedulesAreDiscoveredWithoutUiObservers() = runTest {
    val fixture = Fixture(
      favorites = DirectFavoriteSnapshot(
        teams = listOf(DirectFavorite.Team("direct-team", "Direct team", "")),
        events = listOf(DirectFavorite.Event("event", "Event", "")),
        matches = listOf(DirectFavorite.Match("direct-match", "Direct match", "")),
        players = listOf(DirectFavorite.Player("player", "Player", "")),
      ),
      scheduledMatches = listOf(scheduledMatch("scheduled-match")),
      playerDetails = mapOf("player" to player("player", currentTeamId = "current-team")),
    )

    fixture.useCase()

    assertEquals(1, fixture.matches.overviewRefreshes)
    assertEquals(listOf("player"), fixture.players.refreshes)
    assertEquals(setOf("direct-team", "current-team"), fixture.teams.refreshes.toSet())
    assertEquals(listOf("event"), fixture.events.refreshes)
    assertEquals(setOf("direct-match", "scheduled-match"), fixture.matches.detailRefreshes.toSet())
  }

  @Test
  fun knownCompletedMatchesDoNotRefreshDetails() = runTest {
    val fixture = Fixture(
      favorites = DirectFavoriteSnapshot(
        matches = listOf(
          DirectFavorite.Match("overview-complete", "Completed overview", ""),
          DirectFavorite.Match("detail-final", "Completed detail", ""),
          DirectFavorite.Match("overview-unknown-detail-final", "Final detail with stale overview", ""),
          DirectFavorite.Match("upcoming", "Upcoming", ""),
        ),
      ),
      matchPreviews = listOf(
        matchPreview("overview-complete", MatchStatus.COMPLETED),
        matchPreview("overview-unknown-detail-final", MatchStatus.UNKNOWN),
        matchPreview("upcoming", MatchStatus.UPCOMING),
      ),
      matchDetails = mapOf(
        "detail-final" to matchDetails("detail-final", eventStatus = "FINAL"),
        "overview-unknown-detail-final" to matchDetails("overview-unknown-detail-final", eventStatus = "FINAL"),
      ),
    )

    fixture.useCase()

    assertEquals(listOf("upcoming"), fixture.matches.detailRefreshes)
  }

  @Test
  fun refreshFailuresThrowAndCancellationIsPropagated() = runTest {
    val failure = IllegalStateException("overview failed")
    val failedFixture = Fixture(
      favorites = DirectFavoriteSnapshot(matches = listOf(DirectFavorite.Match("match", "Match", ""))),
    )
    failedFixture.matches.overviewResult = Result.failure(failure)

    assertSame(failure, assertFailsWith<IllegalStateException> { failedFixture.useCase() })

    val cancellation = CancellationException("cancel widget refresh")
    val cancelledFixture = Fixture(
      favorites = DirectFavoriteSnapshot(players = listOf(DirectFavorite.Player("player", "Player", ""))),
    )
    cancelledFixture.players.result = Result.failure(cancellation)

    assertSame(cancellation, assertFailsWith<CancellationException> { cancelledFixture.useCase() })
  }
}

private class Fixture(
  favorites: DirectFavoriteSnapshot = DirectFavoriteSnapshot(),
  scheduledMatches: List<FavoriteScheduledMatch> = emptyList(),
  matchPreviews: List<MatchPreview> = emptyList(),
  matchDetails: Map<String, MatchDetails> = emptyMap(),
  playerDetails: Map<String, PlayerInfo> = emptyMap(),
) {
  val matches = FakeMatchRepository(matchPreviews, matchDetails)
  val teams = FakeTeamRepository()
  val events = FakeEventRepository()
  val players = FakePlayerRepository(playerDetails)

  val useCase = RefreshFavoriteMatches(
    favorites = FakeFavoritesRepository(favorites),
    matches = matches,
    teams = teams,
    events = events,
    players = players,
    schedule = FakeFavoriteScheduleRepository(scheduledMatches),
  )
}

private class FakeFavoritesRepository(
  private val favorites: DirectFavoriteSnapshot,
) : FavoritesRepository {
  override fun observeDirectFavorites(): Flow<DirectFavoriteSnapshot> = flowOf(favorites)

  override fun observeTeamIds(): Flow<Set<String>> = flowOf(favorites.teams.mapTo(mutableSetOf()) { it.id })

  override fun observePlayerIds(): Flow<Set<String>> = flowOf(favorites.players.mapTo(mutableSetOf()) { it.id })
}

private class FakeFavoriteScheduleRepository(
  private val matches: List<FavoriteScheduledMatch>,
) : FavoriteScheduleRepository {
  override fun observeMatches(): Flow<List<FavoriteScheduledMatch>> = flowOf(matches)
}

private class FakeMatchRepository(
  private val matches: List<MatchPreview>,
  private val details: Map<String, MatchDetails>,
) : MatchRepository {
  var overviewRefreshes = 0
  var overviewResult: Result<Unit> = Result.success(Unit)
  val detailRefreshes = mutableListOf<String>()

  override fun getMatches(): Flow<List<MatchPreview>> = flowOf(matches)

  override fun getMatchDetails(matchId: String): Flow<MatchDetails?> = flowOf(details[matchId])

  override suspend fun addToFavorites(matchId: String): Result<Unit> = Result.success(Unit)

  override suspend fun removeFromFavorites(matchId: String): Result<Unit> = Result.success(Unit)

  override suspend fun refreshMatches(): Result<Unit> {
    overviewRefreshes++
    return overviewResult
  }

  override suspend fun refreshMatchDetails(matchId: String): Result<Unit> {
    detailRefreshes += matchId
    return Result.success(Unit)
  }
}

private class FakeTeamRepository : TeamRepository {
  val refreshes = mutableListOf<String>()

  override fun getTeams(): Flow<List<TeamInfo>> = flowOf(emptyList())

  override fun getTeamDetails(teamId: String): Flow<TeamInfo?> = flowOf(null)

  override fun getTeamsByRegion(region: String): Flow<List<TeamInfo>> = flowOf(emptyList())

  override suspend fun addToFavorites(teamId: String): Result<Unit> = Result.success(Unit)

  override suspend fun removeFromFavorites(teamId: String): Result<Unit> = Result.success(Unit)

  override suspend fun refreshTeamDetails(teamId: String): Result<Unit> {
    refreshes += teamId
    return Result.success(Unit)
  }
}

private class FakeEventRepository : EventRepository {
  val refreshes = mutableListOf<String>()

  override fun getEvents(): Flow<List<EventPreview>> = flowOf(emptyList())

  override fun getEventDetails(eventId: String): Flow<EventDetails?> = flowOf(null)

  override suspend fun addToFavorites(eventId: String): Result<Unit> = Result.success(Unit)

  override suspend fun removeFromFavorites(eventId: String): Result<Unit> = Result.success(Unit)

  override suspend fun refreshEvents(): Result<Unit> = Result.success(Unit)

  override suspend fun refreshEventDetails(eventId: String): Result<Unit> {
    refreshes += eventId
    return Result.success(Unit)
  }
}

private class FakePlayerRepository(
  private val details: Map<String, PlayerInfo>,
) : PlayerRepository {
  val refreshes = mutableListOf<String>()
  var result: Result<Unit> = Result.success(Unit)

  override fun getPlayerInTeam(teamId: String): Flow<List<PlayerInfo?>> = flowOf(emptyList())

  override fun getPlayerDetails(playerId: String): Flow<PlayerInfo?> =
    flowOf(details[playerId]?.takeIf { playerId in refreshes })

  override suspend fun addToFavorites(playerId: String): Result<Unit> = Result.success(Unit)

  override suspend fun removeFromFavorites(playerId: String): Result<Unit> = Result.success(Unit)

  override suspend fun refreshPlayerDetails(playerId: String): Result<Unit> {
    refreshes += playerId
    return result
  }
}

private fun matchPreview(id: String, status: MatchStatus): MatchPreview = MatchPreview(
  id = id,
  event = "Event",
  series = "Bo3",
  status = status,
  team1 = TeamPreview(null, "Alpha", "", "", null, null),
  team2 = TeamPreview(null, "Bravo", "", "", null, null),
  time = null,
  eventId = "event",
)

private fun matchDetails(id: String, eventStatus: String): MatchDetails = MatchDetails(
  id = id,
  event = EventInfo("event", "Event", "Series", "Stage", "", null, null, eventStatus),
  head2head = emptyList(),
  note = "",
  score = "",
  teams = emptyList(),
  bans = emptyList(),
  videos = MatchVideos(emptyList(), emptyList()),
  matchData = emptyList(),
  mapCount = 0,
)

private fun scheduledMatch(id: String): FavoriteScheduledMatch = FavoriteScheduledMatch(
  id = id,
  event = "Event",
  team1 = "Alpha",
  team2 = "Bravo",
  time = null,
  status = MatchStatus.UPCOMING,
  score1 = null,
  score2 = null,
  format = "Bo3",
)

private fun player(id: String, currentTeamId: String): PlayerInfo = PlayerInfo(
  id = id,
  name = "Player",
  alias = "player",
  realName = null,
  country = "",
  imageUrl = "",
  twitterUrl = null,
  twitchUrl = null,
  totalWinnings = 0.0,
  currentTeam = PlayerTeam(currentTeamId, "Current team", "", true),
  pastTeams = emptyList(),
  agentStats = emptyList(),
)
