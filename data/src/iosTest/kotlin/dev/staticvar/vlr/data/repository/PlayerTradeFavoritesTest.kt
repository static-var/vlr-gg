/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.match.MatchDataSource
import dev.staticvar.vlr.remotesource.match.MatchDetailsDto
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.TeamDto
import dev.staticvar.vlr.remotesource.player.PlayerDataSource
import dev.staticvar.vlr.remotesource.player.PlayerDetailsDto
import dev.staticvar.vlr.remotesource.player.PlayerTeamRefDto
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerTradeFavoritesTest {
  private val dispatcher = StandardTestDispatcher()
  private val dispatchers = TestDispatcherProvider(dispatcher)
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var playerDataSource: FakePlayerDataSource
  private lateinit var players: PlayerRepositoryImpl
  private lateinit var matches: MatchRepositoryImpl
  private lateinit var favorites: FavoritesRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    playerDataSource = FakePlayerDataSource()
    players = PlayerRepositoryImpl(playerDataSource, database, dispatchers)
    matches = MatchRepositoryImpl(FakeMatchDataSource(), database, dispatchers)
    favorites = FavoritesRepositoryImpl(database, dispatchers)
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun apiTradeMovesLiveMatchFavoritesToNewTeamWithoutCreatingTeamFavorites() = runTest(dispatcher) {
    prepareFavoritePlayer()
    matches.getMatches()
      .map { list -> list.filter { it.isFavorite }.map { it.id }.toSet() }
      .distinctUntilChanged()
      .test {
        assertEquals(setOf("old-team-match"), awaitItem())

        refreshPlayer(currentTeam = newTeam, pastTeams = listOf(oldTeam))

        assertEquals(setOf("new-team-match"), awaitItem())
        assertMatchReasons("old-team-match", emptySet())
        assertMatchReasons("new-team-match", setOf(MatchFavoriteSource.PLAYER))
        assertEquals(setOf("player"), favorites.observePlayerIds().first())
        assertEquals(emptySet(), favorites.observeTeamIds().first())
        val direct = favorites.observeDirectFavorites().first()
        assertEquals(listOf("player"), direct.players.map { it.id })
        assertTrue(direct.teams.isEmpty())
        val player = requireNotNull(players.getPlayerDetails("player").first())
        assertEquals("new-team", player.currentTeam?.id)
        assertTrue(player.isFavorite)
        cancelAndIgnoreRemainingEvents()
      }
  }

  @Test
  fun directOldTeamFavoriteSurvivesPlayerTradeAsSeparateReason() = runTest(dispatcher) {
    prepareFavoritePlayer()
    database.teamsQueries.addFavoriteTeam("old-team")
    assertMatchReasons("old-team-match", setOf(MatchFavoriteSource.TEAM, MatchFavoriteSource.PLAYER))

    refreshPlayer(currentTeam = newTeam, pastTeams = listOf(oldTeam))

    assertEquals(
      setOf("old-team-match", "new-team-match"),
      matches.getMatches().first().filter { it.isFavorite }.map { it.id }.toSet(),
    )
    assertMatchReasons("old-team-match", setOf(MatchFavoriteSource.TEAM))
    assertMatchReasons("new-team-match", setOf(MatchFavoriteSource.PLAYER))
    assertEquals(setOf("old-team"), favorites.observeTeamIds().first())
    assertEquals(setOf("player"), favorites.observePlayerIds().first())
  }

  @Test
  fun freeAgentResponseClearsInheritedMatchesAndFailedRefreshKeepsLastKnownTeam() = runTest(dispatcher) {
    prepareFavoritePlayer()
    playerDataSource.response = Result.failure(IllegalStateException("Offline"))
    assertTrue(players.refreshPlayerDetails("player").isFailure)
    assertMatchReasons("old-team-match", setOf(MatchFavoriteSource.PLAYER))

    refreshPlayer(currentTeam = null, pastTeams = listOf(oldTeam))

    assertTrue(matches.getMatches().first().none { it.isFavorite })
    assertEquals(null, requireNotNull(players.getPlayerDetails("player").first()).currentTeam)
    assertEquals(setOf("player"), favorites.observePlayerIds().first())
    assertEquals(emptySet(), favorites.observeTeamIds().first())
  }

  private suspend fun prepareFavoritePlayer() {
    assertTrue(matches.refreshMatches().isSuccess)
    refreshPlayer(currentTeam = oldTeam)
    assertTrue(players.addToFavorites("player").isSuccess)
  }

  private suspend fun refreshPlayer(currentTeam: PlayerTeamRefDto?, pastTeams: List<PlayerTeamRefDto> = emptyList()) {
    playerDataSource.response = Result.success(
      PlayerDetailsDto(name = "Favorite Player", alias = "Ace", currentTeam = currentTeam, pastTeams = pastTeams),
    )
    assertTrue(players.refreshPlayerDetails("player").isSuccess)
  }

  private suspend fun assertMatchReasons(matchId: String, expected: Set<MatchFavoriteSource>) {
    val match = matches.getMatches().first().single { it.id == matchId }
    assertEquals(expected, match.favoriteReasons.map { it.source }.toSet())
    assertEquals(expected.isNotEmpty(), match.isFavorite)
    assertEquals(false, match.isDirectFavorite)
    match.favoriteReasons.filter { it.source == MatchFavoriteSource.PLAYER }.forEach {
      assertEquals("player", it.id)
    }
  }

  private class FakePlayerDataSource : PlayerDataSource {
    var response: Result<PlayerDetailsDto> = Result.failure(IllegalStateException("No response"))
    override suspend fun details(id: String): Result<PlayerDetailsDto> = response
  }

  private class FakeMatchDataSource : MatchDataSource {
    override suspend fun list(): Result<List<MatchPreviewDto>> = Result.success(
      listOf("old-team", "new-team", "unrelated-team").map { teamId ->
        MatchPreviewDto(
          id = "$teamId-match",
          event = "Champions",
          eventId = "event",
          series = "Playoffs",
          status = MatchStatus.UPCOMING,
          team1 = TeamDto(id = teamId, name = teamId),
          team2 = TeamDto(id = "opponent", name = "Opponent"),
          time = "2026-09-13T12:00:00Z",
        )
      },
    )

    override suspend fun details(id: String): Result<MatchDetailsDto> =
      Result.failure(IllegalStateException("Details were not requested"))
  }

  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }

  private companion object {
    val oldTeam = PlayerTeamRefDto(id = "old-team", name = "Old Team")
    val newTeam = PlayerTeamRefDto(id = "new-team", name = "New Team")
  }
}
