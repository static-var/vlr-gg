/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.Players
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RankingFavoritesQueriesTest {
  private lateinit var driver: NativeSqliteDriver
  private lateinit var database: VlrDatabase

  @BeforeTest
  fun setUp() {
    driver = inMemoryDriver(VlrDatabase.Schema)
    database = VlrDatabase(driver)
  }

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  @Test
  fun rankingsObserveExplicitTeamFavoritesWithoutRefreshing() = runTest {
    database.rankingsQueries.insertRanking("120", "NA", 1, "100", 0)
    database.rankingsQueries.insertRanking("other", "EU", 1, "90", 0)
    database.playersQueries.insertPlayer(
      Players(
        id = "601", name = "Asuna", alias = "Asuna", real_name = null, country = "US",
        current_team_id = "120", image_url = null, twitter_url = null, twitch_url = null,
        total_winnings = 0.0, last_updated = 0,
      ),
    )
    database.playersQueries.addFavoritePlayer("601")
    var allFavorites = emptyList<Long>()
    var regionalFavorites = emptyList<Long>()
    val dispatcher = StandardTestDispatcher(testScheduler)
    backgroundScope.launch(dispatcher) {
      database.rankingsQueries.getRankingsWithFavoriteStatus(null).asFlow().mapToList(dispatcher).collect {
        allFavorites = it.map { row -> row.is_favorite }
      }
    }
    backgroundScope.launch(dispatcher) {
      database.rankingsQueries.getRankingsWithFavoriteStatus("NA").asFlow().mapToList(dispatcher).collect {
        regionalFavorites = it.map { row -> row.is_favorite }
      }
    }
    runCurrent()
    assertEquals(listOf(0L, 0L), allFavorites)
    assertEquals(listOf(0L), regionalFavorites)

    database.teamsQueries.addFavoriteTeam("120")
    runCurrent()
    assertEquals(listOf(0L, 1L), allFavorites)
    assertEquals(listOf(1L), regionalFavorites)

    database.teamsQueries.removeFavoriteTeam("120")
    runCurrent()
    assertEquals(listOf(0L, 0L), allFavorites)
    assertEquals(listOf(0L), regionalFavorites)
  }
}
