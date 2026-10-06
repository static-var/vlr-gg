/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.domain.model.RankingRegion
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.RankingRecordDto
import dev.staticvar.vlr.remotesource.rankings.RankingTeamDto
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import dev.staticvar.vlr.remotesource.rankings.TeamRankingDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RankingsRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeRankingsDataSource
  private lateinit var repository: RankingsRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)

    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakeRankingsDataSource()
    repository = RankingsRepositoryImpl(
      rankingsDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider,
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun refreshRankings_replacesRankingInApiOrder() = runTest(dispatcher) {
    insertRanking("old", position = 0)

    dataSource.listResult = Result.success(
      listOf(
        TeamRankingDto(
          rank = 1,
          team = RankingTeamDto(id = "11", name = "Alpha", logo = "alpha.png", country = "United States"),
          elo = 1850.4,
          matches = RankingRecordDto(wins = 20, losses = 4),
          region = "americas",
        ),
        TeamRankingDto(
          rank = 2,
          team = RankingTeamDto(id = "21", name = "Gamma"),
          elo = 1800.0,
          matches = RankingRecordDto(wins = 9, losses = 9),
          region = "unclassified",
        ),
        TeamRankingDto(
          rank = 2,
          team = RankingTeamDto(id = "12", name = "Beta"),
          elo = 1800.0,
          matches = RankingRecordDto(wins = 8, losses = 8),
        ),
      ),
    )

    val result = repository.refreshRankings()
    assertTrue(result.isSuccess)

    val rows = database.rankingsQueries.getAllRankings().executeAsList()
    assertEquals(listOf("11", "21", "12"), rows.map { it.team_id })
    assertEquals(listOf(1L, 2L, 2L), rows.map { it.rank })
    assertEquals(listOf("americas", null, null), rows.map { it.region })
    val alpha = rows.first()
    assertEquals("Alpha", alpha.team_name)
    assertEquals("alpha.png", alpha.team_logo)
    assertEquals("United States", alpha.country)
    assertEquals(1850.4, alpha.elo)
    assertEquals(20L, alpha.match_wins)
    assertEquals(4L, alpha.match_losses)
    assertEquals("", rows[1].team_logo)
    assertEquals("", rows[1].country)

    repository.getRankings().test {
      assertEquals(listOf(RankingRegion.Americas, null, null), awaitItem().map { it.region })
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun refreshRankings_failureKeepsCachedRanking() = runTest(dispatcher) {
    insertRanking("cached", position = 0)
    dataSource.listResult = Result.failure(IllegalStateException("offline"))

    assertTrue(repository.refreshRankings().isFailure)

    assertEquals(listOf("cached"), database.rankingsQueries.getAllRankings().executeAsList().map { it.team_id })
  }

  @Test
  fun getRankings_emitsTeamsInPositionOrderWithFavorites() = runTest(dispatcher) {
    insertRanking("team2", position = 1, rank = 2, elo = 1790.0, region = "emea")
    insertRanking("team1", position = 0, rank = 1, elo = 1835.6, region = "pacific")
    insertRanking("team3", position = 2, rank = 3, region = "china")
    database.teamsQueries.addFavoriteTeam("team2")

    repository.getRankings().test {
      val emission = awaitItem()
      assertEquals(listOf("team1", "team2", "team3"), emission.map { it.teamId })
      assertEquals(listOf(1, 2, 3), emission.map { it.rank })
      assertEquals(1835.6, emission.first().elo)
      assertEquals(listOf(false, true, false), emission.map { it.isFavorite })
      assertEquals(listOf(RankingRegion.Pacific, RankingRegion.Emea, RankingRegion.China), emission.map { it.region })
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun insertRanking(
    teamId: String,
    position: Long,
    rank: Long = position + 1,
    elo: Double = 1500.0,
    region: String? = null,
  ) {
    database.rankingsQueries.insertRanking(
      team_id = teamId,
      team_name = "Team $teamId",
      team_logo = "",
      country = "",
      rank = rank,
      position = position,
      elo = elo,
      match_wins = 0,
      match_losses = 0,
      last_updated = 0,
      region = region,
    )
  }

  private class FakeRankingsDataSource : RankingsDataSource {
    var listResult: Result<List<TeamRankingDto>> = Result.success(emptyList())
    override suspend fun list(): Result<List<TeamRankingDto>> = listResult
  }
  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
}
