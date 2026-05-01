package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.rankings.RankingDto
import dev.staticvar.vlr.remotesource.rankings.RankingsDataSource
import dev.staticvar.vlr.remotesource.rankings.TeamRankingDto
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RankingsRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: JdbcSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeRankingsDataSource
  private lateinit var repository: RankingsRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VlrDatabase.Schema.create(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakeRankingsDataSource()
    repository = RankingsRepositoryImpl(
      rankingsDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun refreshRankings_replacesDataPerRegion() = runTest(dispatcher) {
    database.rankingsQueries.insertRanking("old1", "NA", 5L, "25", 0L)
    database.rankingsQueries.insertRanking("old2", "EU", 3L, "30", 0L)

    dataSource.listResult = Result.success(
      listOf(
        RankingDto(
          region = "NA",
          teams = listOf(
            TeamRankingDto(id = 11, name = "Alpha", rank = 1, points = 120, country = "US"),
            TeamRankingDto(id = 12, name = "Beta", rank = 2, points = 100, country = "CA")
          )
        ),
        RankingDto(
          region = "APAC",
          teams = listOf(
            TeamRankingDto(id = 21, name = "Gamma", rank = 1, points = 140, country = "KR")
          )
        )
      )
    )

    val result = repository.refreshRankings()
    assertTrue(result.isSuccess)

    val na = database.rankingsQueries.getRankingsByRegion("NA").executeAsList()
    assertEquals(listOf(1, 2), na.map { it.rank.toInt() })
    val apac = database.rankingsQueries.getRankingsByRegion("APAC").executeAsList()
    assertEquals(1, apac.size)
    assertTrue(database.rankingsQueries.getRankingsByRegion("EU").executeAsList().isEmpty())
  }

  @Test
  fun getAllRankings_groupsByRegion() = runTest(dispatcher) {
    database.rankingsQueries.insertRanking("team1", "NA", 1L, "100", 0L)
    database.rankingsQueries.insertRanking("team2", "NA", 2L, "80", 0L)
    database.rankingsQueries.insertRanking("team3", "EMEA", 1L, "90", 0L)

    repository.getAllRankings().test {
      val emission = awaitItem()
      assertEquals(2, emission.size)
      val na = emission.first { it.region == "NA" }
      assertEquals(listOf(1, 2), na.teams.map { it.rank })
      val emea = emission.first { it.region == "EMEA" }
      assertEquals(listOf("team3"), emea.teams.map { it.teamId })
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun getRankingsByRegion_emitsNullWhenEmpty() = runTest(dispatcher) {
    repository.getRankingsByRegion("LATAM").test {
      assertTrue(awaitItem() == null)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private class FakeRankingsDataSource : RankingsDataSource {
    var listResult: Result<List<RankingDto>> = Result.success(emptyList())
    override suspend fun list(): Result<List<RankingDto>> = listResult
  }

  private class TestDispatcherProvider(
    private val dispatcher: TestDispatcher
  ) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
}
