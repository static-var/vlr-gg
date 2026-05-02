package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.data.Teams
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.domain.model.TeamInfo
import dev.staticvar.vlr.remotesource.team.TeamDataSource
import dev.staticvar.vlr.remotesource.team.TeamDetailsDto
import dev.staticvar.vlr.remotesource.team.TeamPlayerDto
import dev.staticvar.vlr.remotesource.team.UpcomingMatchDto
import dev.staticvar.vlr.remotesource.team.CompletedMatchDto
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TeamRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: JdbcSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeTeamDataSource
  private lateinit var repository: TeamRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VlrDatabase.Schema.create(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakeTeamDataSource()
    repository = TeamRepositoryImpl(
      teamDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun getTeams_filtersAndFavorites() = runTest(dispatcher) {
    insertTeam(
      id = "team1",
      name = "Team One",
      tag = "ONE",
      region = "NA",
      country = "US",
      rank = 1
    )
    insertTeam(
      id = "team2",
      name = "Team Two",
      tag = "TWO",
      region = "EU",
      country = "DE",
      rank = 2
    )
    assertTrue(repository.addToFavorites("team2").isSuccess)

    repository.getTeamsByRegion("NA").test {
      val emission = awaitItem()
      assertEquals(listOf("team1"), emission.map { it.id })
      cancelAndIgnoreRemainingEvents()
    }

    repository.getTeams().test {
      val emission = awaitItem()
      val favorite = emission.first { it.id == "team2" }
      assertTrue(favorite.isFavorite)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun teamDetails_flow_updatesAfterRefresh() = runTest(dispatcher) {
    insertTeam(
      id = "team1",
      name = "Team One",
      tag = "ONE",
      region = "NA",
      country = "US",
      rank = 1
    )

    dataSource.detailResults["team1"] = Result.success(
      TeamDetailsDto(
        name = "Team One",
        tag = "ONE",
        img = "team.png",
        website = "https://team.one",
        twitter = "@teamone",
        country = "US",
        rank = 1,
        region = "NA",
        roster = listOf(
          TeamPlayerDto(
            id = "player1",
            name = "Player One",
            alias = "p1",
            role = "Duelist",
            img = "p1.png"
          )
        ),
        upcoming = listOf(
          UpcomingMatchDto(
            id = "match1",
            event = "Event",
            stage = "Stage",
            opponent = "Opponent",
            date = "2025-01-01",
            eta = "2h"
          )
        ),
        completed = listOf(
          CompletedMatchDto(
            id = "match2",
            event = "Past Event",
            stage = "Final",
            opponent = "Old Opponent",
            date = "2024-12-01",
            score = "2-1"
          )
        )
      )
    )

    repository.getTeamDetails("team1").test {
      val initial = awaitItem()
      assertEquals("Team One", initial?.name)

      val refreshResult = repository.refreshTeamDetails("team1")
      assertTrue(refreshResult.isSuccess)
      advanceUntilIdle()
      assertEquals(1, database.teamsQueries.getTeamRoster("team1").executeAsList().size)
      assertEquals(1, database.teamsQueries.getUpcomingMatches("team1").executeAsList().size)
      assertEquals(1, database.teamsQueries.getCompletedMatches("team1").executeAsList().size)

      var updated: TeamInfo? = null
      loop@ for (i in 0 until 6) {
        val emission = awaitItem()
        if (
          emission?.roster?.isNotEmpty() == true &&
          emission.upcomingMatches.isNotEmpty() &&
          emission.completedMatches.isNotEmpty()
        ) {
          updated = emission
          cancelAndIgnoreRemainingEvents()
          break@loop
        }
      }

      val details = requireNotNull(updated) { "No emission with populated roster" }
      assertEquals(1, details.roster.size)
      assertEquals("Player One", details.roster.first().name)
      assertEquals("p1", details.roster.first().alias)
      assertEquals("p1.png", details.roster.first().imageUrl)
      assertEquals(1, details.upcomingMatches.size)
      assertEquals("Stage", details.upcomingMatches.first().stage)
      assertEquals("2h", details.upcomingMatches.first().eta)
      assertEquals(1, details.completedMatches.size)
      assertEquals("Final", details.completedMatches.first().stage)
    }
  }

  @Test
  fun favorites_areUpdated() = runTest(dispatcher) {
    insertTeam(
      id = "team1",
      name = "Favorite",
      tag = "FAV",
      region = "NA",
      country = "US",
      rank = 1
    )

    assertTrue(repository.addToFavorites("team1").isSuccess)

    repository.getTeams().test {
      val emission = awaitItem()
      val favorite = emission.first { it.id == "team1" }
      assertTrue(favorite.isFavorite)
      cancelAndIgnoreRemainingEvents()
    }

    assertTrue(repository.removeFromFavorites("team1").isSuccess)

    repository.getTeams().test {
      val emission = awaitItem()
      val updated = emission.first { it.id == "team1" }
      assertTrue(!updated.isFavorite)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun insertTeam(
    id: String,
    name: String,
    tag: String,
    region: String,
    country: String,
    rank: Int
  ) {
    database.teamsQueries.insertTeam(
      Teams(
        id = id,
        name = name,
        tag = tag,
        logo_url = "$id.png",
        region = region,
        country = country,
        roster_url = null,
        earnings = null,
        rank = rank.toLong(),
        website = null,
        twitter = null,
        last_updated = 0
      )
    )
  }

  private class TestDispatcherProvider(
    private val dispatcher: TestDispatcher
  ) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }

  private class FakeTeamDataSource : TeamDataSource {
    val detailResults: MutableMap<String, Result<TeamDetailsDto>> = mutableMapOf()

    override suspend fun details(id: String): Result<TeamDetailsDto> =
      detailResults[id] ?: Result.failure(IllegalStateException("No details for $id"))
  }
}
