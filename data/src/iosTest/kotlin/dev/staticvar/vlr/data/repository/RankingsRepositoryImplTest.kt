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
import dev.staticvar.vlr.domain.model.RankingCircuit
import dev.staticvar.vlr.domain.model.RankingMetric
import dev.staticvar.vlr.domain.model.RankingOrder
import dev.staticvar.vlr.domain.model.RankingsQuery
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.RankingRecordDto
import dev.staticvar.vlr.remotesource.rankings.RankingTeamDto
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import dev.staticvar.vlr.remotesource.rankings.RankingsRequest
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
          matches = RankingRecordDto(wins = 20, losses = 4, played = 24, winRate = 0.8333),
          region = "americas",
          mapElo = 1752.1,
          overallRank = 7,
        ),
        TeamRankingDto(
          rank = 2,
          team = RankingTeamDto(id = "21", name = "Gamma"),
          elo = 1800.0,
          matches = RankingRecordDto(wins = 9, losses = 9, played = 18, winRate = 0.5),
          region = "unclassified",
          mapElo = 1790.0,
        ),
        TeamRankingDto(
          rank = 2,
          team = RankingTeamDto(id = "12", name = "Beta"),
          elo = 1800.0,
          matches = RankingRecordDto(wins = 8, losses = 8, played = 16, winRate = 0.5),
          mapElo = 1790.0,
        ),
      ),
    )

    val result = repository.refreshRankings()
    assertTrue(result.isSuccess)

    val rows = database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus(RankingsQuery().cacheKey).executeAsList()
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
    assertEquals(1752.1, alpha.map_elo)
    assertEquals(24L, alpha.matches_played)
    assertEquals(0.8333, alpha.win_rate)
    assertEquals(7L, alpha.overall_rank)
    assertEquals("", rows[1].team_logo)
    assertEquals("", rows[1].country)

    repository.getRankings().test {
      val teams = awaitItem()
      assertEquals(listOf(RankingRegion.Americas, null, null), teams.map { it.region })
      assertEquals(1752.1, teams.first().mapElo)
      assertEquals(24, teams.first().matchesPlayed)
      assertEquals(0.8333, teams.first().winRate)
      assertEquals(7, teams.first().overallRank)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun refreshRankings_failureKeepsCachedRanking() = runTest(dispatcher) {
    insertRanking("cached", position = 0)
    dataSource.listResult = Result.failure(IllegalStateException("offline"))

    assertTrue(repository.refreshRankings().isFailure)

    assertEquals(listOf("cached"), database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus(RankingsQuery().cacheKey).executeAsList().map { it.team_id })
  }

  @Test
  fun refreshRankings_isolates_every_query_dimension_and_preserves_other_selections() = runTest(dispatcher) {
    val original = RankingsQuery()
    val variants = listOf(
      original.copy(circuit = RankingCircuit.GameChangers),
      original.copy(region = RankingRegion.Emea),
      original.copy(minMatches = 0),
      original.copy(includeInactive = true),
      original.copy(metric = RankingMetric.MapElo),
      original.copy(order = RankingOrder.Asc),
    )
    insertRanking("original", 0, query = original)
    for ((index, query) in variants.withIndex()) {
      repository.getRankings(query).test {
        assertTrue(awaitItem().isEmpty())
        dataSource.listResult = Result.success(listOf(
          TeamRankingDto(1, RankingTeamDto("selected-$index", "Selected"), 1600.0, RankingRecordDto(4, 1, 5, 0.8), mapElo = 1590.0),
        ))
        assertTrue(repository.refreshRankings(query).isSuccess)
        assertEquals(listOf("selected-$index"), awaitItem().map { it.teamId })
        cancelAndIgnoreRemainingEvents()
      }
      assertEquals(
        RankingsRequest(query.circuit.apiValue, query.region?.apiValue ?: "all", query.minMatches, query.includeInactive, query.metric.apiValue, query.order.apiValue),
        dataSource.queries.last(),
      )
    }
    repository.getRankings(original).test {
      assertEquals(listOf("original"), awaitItem().map { it.teamId })
      cancelAndIgnoreRemainingEvents()
    }
    for ((index, query) in variants.withIndex()) {
      repository.getRankings(query).test {
        assertEquals(listOf("selected-$index"), awaitItem().map { it.teamId })
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Test
  fun refreshRankings_empty_selection_does_not_clear_another_query() = runTest(dispatcher) {
    val regional = RankingsQuery(region = RankingRegion.China)
    insertRanking("global", 0)
    insertRanking("regional", 0, query = regional)

    assertTrue(repository.refreshRankings(regional).isSuccess)

    assertTrue(database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus(regional.cacheKey).executeAsList().isEmpty())
    assertEquals(listOf("global"), database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus(RankingsQuery().cacheKey).executeAsList().map { it.team_id })
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
    query: RankingsQuery = RankingsQuery(),
  ) {
    database.rankingsQueries.insertRankingQueryTeam(
      query_key = query.cacheKey,
      team_id = teamId,
      team_name = "Team $teamId",
      team_logo = "",
      country = "",
      rank = rank,
      overall_rank = rank,
      position = position,
      elo = elo,
      map_elo = elo,
      matches_played = 0,
      win_rate = 0.0,
      match_wins = 0,
      match_losses = 0,
      last_updated = 0,
      region = region,
    )
  }

  private class FakeRankingsDataSource : RankingsDataSource {
    var listResult: Result<List<TeamRankingDto>> = Result.success(emptyList())
    val queries = mutableListOf<RankingsRequest>()
    override suspend fun list(query: RankingsRequest): Result<List<TeamRankingDto>> {
      queries += query
      return listResult
    }
  }
  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
}
