/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
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
class FavoritesQueriesTest {
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
  fun favoriteIdsUpdateWithoutCachedTeamsOrPlayers() = runTest {
    database.teamsQueries.addFavoriteTeam("team-1")
    database.teamsQueries.addFavoriteTeam("team-2")
    database.playersQueries.addFavoritePlayer("player-1")
    database.playersQueries.addFavoritePlayer("player-2")
    assertEquals(emptyList(), database.teamsQueries.getTeamsWithFavoriteStatus().executeAsList())
    assertEquals(emptyList(), database.playersQueries.getPlayersWithFavoriteStatus().executeAsList())
    var teamIds = emptySet<String>()
    var playerIds = emptySet<String>()
    val dispatcher = StandardTestDispatcher(testScheduler)
    backgroundScope.launch(dispatcher) {
      database.teamsQueries.getFavoriteTeamIds().asFlow().mapToList(dispatcher).collect {
        teamIds = it.toSet()
      }
    }
    backgroundScope.launch(dispatcher) {
      database.playersQueries.getFavoritePlayerIds().asFlow().mapToList(dispatcher).collect {
        playerIds = it.toSet()
      }
    }
    runCurrent()
    assertEquals(setOf("team-1", "team-2"), teamIds)
    assertEquals(setOf("player-1", "player-2"), playerIds)

    database.teamsQueries.removeFavoriteTeam("team-1")
    database.playersQueries.removeFavoritePlayer("player-1")
    runCurrent()
    assertEquals(setOf("team-2"), teamIds)
    assertEquals(setOf("player-2"), playerIds)

    database.teamsQueries.addFavoriteTeam("team-3")
    database.playersQueries.removeFavoritePlayer("player-2")
    runCurrent()
    assertEquals(setOf("team-2", "team-3"), teamIds)
    assertEquals(emptySet(), playerIds)
  }
}
