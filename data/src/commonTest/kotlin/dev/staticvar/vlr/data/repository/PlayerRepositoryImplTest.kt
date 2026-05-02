/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.Players
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.player.PlayerAgentStatsDto
import dev.staticvar.vlr.remotesource.player.PlayerDataSource
import dev.staticvar.vlr.remotesource.player.PlayerDetailsDto
import dev.staticvar.vlr.remotesource.player.PlayerTeamRefDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: JdbcSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakePlayerDataSource
  private lateinit var repository: PlayerRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VlrDatabase.Schema.create(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakePlayerDataSource()
    repository = PlayerRepositoryImpl(
      playerDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider,
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun refreshPlayerDetails_upsertsPlayerAndRelations() = runTest(dispatcher) {
    dataSource.detailsResults["player1"] = Result.success(
      PlayerDetailsDto(
        name = "Player One",
        alias = "p1",
        twitch = "twitch.tv/p1",
        twitter = "@p1",
        country = "BR",
        img = "player.png",
        agents = listOf(
          PlayerAgentStatsDto(name = "Sova", count = 50, percent = 25.0, rounds = 200, rating = 1.1),
        ),
        totalWinnings = 12345.0,
        currentTeam = PlayerTeamRefDto(id = "teamA", name = "Team A", img = "teamA.png"),
        pastTeams = listOf(
          PlayerTeamRefDto(id = "teamB", name = "Team B", img = "teamB.png"),
        ),
      ),
    )

    val result = repository.refreshPlayerDetails("player1")
    assertTrue(result.isSuccess)

    val stored = database.playersQueries.getPlayerWithFavoriteStatus("player1").executeAsOne()
    assertEquals("Player One", stored.name)
    val agents = database.playersQueries.getPlayerAgentStats("player1").executeAsList()
    assertEquals(1, agents.size)
    val history = database.playersQueries.getPlayerTeamHistory("player1").executeAsList()
    assertEquals(listOf(1L, 0L), history.map { it.is_current })
  }

  @Test
  fun playerTeamFlow_emitsAggregatedInfoAndFavorites() = runTest(dispatcher) {
    database.playersQueries.insertPlayer(
      Players(
        id = "player1",
        name = "Player One",
        alias = "p1",
        real_name = "Real One",
        country = "US",
        current_team_id = "teamA",
        image_url = "player.png",
        twitter_url = "@p1",
        twitch_url = "twitch.tv/p1",
        total_winnings = 5000.0,
        last_updated = 0L,
      ),
    )
    database.playersQueries.insertPlayerAgentStat(
      player_id = "player1",
      agent_name = "Sova",
      agent_image_url = "sova.png",
      usage_count = 30L,
      usage_percent = 15.0,
      rounds_played = 120L,
      rating = 1.05,
      acs = 220.0,
      kd_ratio = 1.2,
      adr = 140.0,
      kast = 75.0,
      kpr = 0.8,
      apr = 0.4,
      fkpr = 0.2,
      fdpr = 0.1,
      kills = 200L,
      deaths = 150L,
      assists = 80L,
      first_kills = 30L,
      first_deaths = 15L,
    )
    database.playersQueries.insertPlayerTeamHistory(
      player_id = "player1",
      team_id = "teamA",
      team_name = "Team A",
      team_logo_url = "teamA.png",
      is_current = 1L,
    )
    assertTrue(repository.addToFavorites("player1").isSuccess)

    repository.getPlayerInTeam("teamA").test {
      val players = awaitItem()
      assertEquals(1, players.size)
      val player = requireNotNull(players.first())
      assertEquals("Player One", player.name)
      assertEquals("Team A", player.currentTeam?.name)
      assertTrue(player.isFavorite)
      assertEquals(1, player.agentStats.size)
      cancelAndIgnoreRemainingEvents()
    }

    assertTrue(repository.removeFromFavorites("player1").isSuccess)
    assertFalse(database.playersQueries.isFavoritePlayer("player1").executeAsOne() > 0L)

    repository.getPlayerInTeam("teamA").test {
      val players = awaitItem()
      val player = requireNotNull(players.first())
      assertFalse(player.isFavorite)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun getPlayerDetails_emitsUpdatesAfterRefresh() = runTest(dispatcher) {
    repository.getPlayerDetails("player2").test {
      assertTrue(awaitItem() == null)

      dataSource.detailsResults["player2"] = Result.success(
        PlayerDetailsDto(
          name = "Player Two",
          alias = "p2",
          country = "CA",
          img = "player2.png",
          agents = listOf(PlayerAgentStatsDto(name = "Jett", count = 60, percent = 30.0)),
          totalWinnings = 999.0,
          currentTeam = PlayerTeamRefDto(id = "teamC", name = "Team C", img = "teamC.png"),
        ),
      )

      val refreshResult = repository.refreshPlayerDetails("player2")
      assertTrue(refreshResult.isSuccess)
      advanceUntilIdle()

      var updated = awaitItem()
      while (updated?.currentTeam == null) {
        updated = awaitItem()
      }
      assertEquals("Player Two", updated.name)
      assertEquals("Team C", updated.currentTeam?.name)
      assertEquals(1, updated.agentStats.size)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private class FakePlayerDataSource : PlayerDataSource {
    val detailsResults = mutableMapOf<String, Result<PlayerDetailsDto>>()
    override suspend fun details(id: String): Result<PlayerDetailsDto> =
      detailsResults[id] ?: Result.failure(IllegalStateException("No details for $id"))
  }

  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
}
