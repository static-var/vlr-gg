/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import dev.staticvar.vlr.core.coroutines.DispatcherProvider
import dev.staticvar.vlr.localsource.database.VlrDatabase
import dev.staticvar.vlr.remotesource.standings.CircuitStandingDto
import dev.staticvar.vlr.remotesource.standings.StandingsDataSource
import dev.staticvar.vlr.remotesource.standings.StandingsDto
import dev.staticvar.vlr.remotesource.standings.TeamStandingDto
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
class CircuitStandingsRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private val dispatcherProvider = TestDispatcherProvider(dispatcher)
  private lateinit var driver: JdbcSqliteDriver
  private lateinit var database: VlrDatabase
  private lateinit var dataSource: FakeStandingsDataSource
  private lateinit var repository: CircuitStandingsRepositoryImpl

  @BeforeTest
  fun setup() {
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VlrDatabase.Schema.create(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    database = VlrDatabase(driver)
    dataSource = FakeStandingsDataSource()
    repository = CircuitStandingsRepositoryImpl(
      standingsDataSource = dataSource,
      database = database,
      dispatchers = dispatcherProvider,
    )
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun refreshStandings_preservesTeamMetadata() = runTest(dispatcher) {
    dataSource.result = Result.success(
      StandingsDto(
        year = 2025,
        circuits = listOf(
          CircuitStandingDto(
            region = "Americas Championship",
            teams = listOf(
              TeamStandingDto(
                id = 11058,
                name = "G2 Esports",
                logo = "g2.png",
                rank = 1,
                points = 30,
                country = "United States",
              ),
            ),
          ),
        ),
      ),
    )

    val result = repository.refreshStandings(2025)
    assertTrue(result.isSuccess)

    repository.getStandingsByYear(2025).test {
      val standings = awaitItem()
      requireNotNull(standings)
      assertEquals(2025, standings.year)
      val team = standings.circuits.single().teams.single()
      assertEquals("11058", team.id)
      assertEquals("G2 Esports", team.name)
      assertEquals("g2.png", team.logo)
      assertEquals("United States", team.country)
      assertEquals(1, team.rank)
      assertEquals(30, team.points)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private class FakeStandingsDataSource : StandingsDataSource {
    var result: Result<StandingsDto> = Result.success(StandingsDto())

    override suspend fun byYear(year: Int): Result<StandingsDto> = result
  }

  private class TestDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val default = dispatcher
    override val io = dispatcher
    override val main = dispatcher
  }
}
