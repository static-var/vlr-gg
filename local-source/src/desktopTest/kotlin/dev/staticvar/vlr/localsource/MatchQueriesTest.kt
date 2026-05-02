/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.staticvar.vlr.localsource.database.DatabaseDispatchers
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MatchQueriesTest {
  private lateinit var driver: SqlDriver
  private lateinit var database: VlrDatabase

  @BeforeTest
  fun setup() {
    // Use in-memory database for tests
    driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    VlrDatabase.Schema.create(driver)
    database = VlrDatabase(driver)

    // Enable foreign key constraints
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun `insert and query match`() {
    val match = createTestMatch(id = "match1")

    database.matchesQueries.insertMatch(match)

    val result = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOne()

    assertEquals("match1", result.id)
    assertEquals("Test Event", result.event_name)
    assertEquals("Team A", result.team1_name)
    assertEquals("Team B", result.team2_name)
    assertEquals(0L, result.is_direct_favorite)
  }

  @Test
  fun `query all matches returns empty list initially`() {
    val matches = database.matchesQueries.getMatchesWithFavoriteStatus().executeAsList()

    assertTrue(matches.isEmpty())
  }

  @Test
  fun `query all matches returns inserted matches`() {
    val match1 = createTestMatch(id = "match1", team1Name = "Team A")
    val match2 = createTestMatch(id = "match2", team1Name = "Team C")

    database.matchesQueries.insertMatch(match1)
    database.matchesQueries.insertMatch(match2)

    val matches = database.matchesQueries.getMatchesWithFavoriteStatus().executeAsList()

    assertEquals(2, matches.size)
  }

  @Test
  fun `insert or replace updates existing match`() {
    val match = createTestMatch(id = "match1", team1Name = "Team A")
    database.matchesQueries.insertMatch(match)

    val updatedMatch = match.copy(team1_name = "Team A Updated")
    database.matchesQueries.insertMatch(updatedMatch)

    val result = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOne()

    assertEquals("Team A Updated", result.team1_name)
  }

  @Test
  fun `delete match by id removes match`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)

    database.matchesQueries.deleteMatchById("match1")

    val result = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOneOrNull()

    assertEquals(null, result)
  }

  @Test
  fun `delete all matches removes all matches`() {
    database.matchesQueries.insertMatch(createTestMatch(id = "match1"))
    database.matchesQueries.insertMatch(createTestMatch(id = "match2"))

    database.matchesQueries.deleteAllMatches()

    val matches = database.matchesQueries.getMatchesWithFavoriteStatus().executeAsList()

    assertTrue(matches.isEmpty())
  }

  @Test
  fun `add favorite match marks match as favorite`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)

    database.matchesQueries.addFavoriteMatch("match1")

    val result = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOne()

    assertEquals(1L, result.is_direct_favorite)
  }

  @Test
  fun `remove favorite match unmarks match as favorite`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)
    database.matchesQueries.addFavoriteMatch("match1")

    database.matchesQueries.removeFavoriteMatch("match1")

    val result = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOne()

    assertEquals(0L, result.is_direct_favorite)
  }

  @Test
  fun `is favorite match returns correct count`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)

    val notFavorite = database.matchesQueries.isFavoriteMatch("match1").executeAsOne()
    assertEquals(0, notFavorite)

    database.matchesQueries.addFavoriteMatch("match1")

    val isFavorite = database.matchesQueries.isFavoriteMatch("match1").executeAsOne()
    assertEquals(1, isFavorite)
  }

  @Test
  fun `get all favorite matches returns only favorites`() {
    database.matchesQueries.insertMatch(createTestMatch(id = "match1"))
    database.matchesQueries.insertMatch(createTestMatch(id = "match2"))
    database.matchesQueries.insertMatch(createTestMatch(id = "match3"))

    database.matchesQueries.addFavoriteMatch("match1")
    database.matchesQueries.addFavoriteMatch("match3")

    val favorites = database.matchesQueries.getAllFavoriteMatches().executeAsList()

    assertEquals(2, favorites.size)
    assertTrue(favorites.all { it.is_direct_favorite == 1L })
  }

  @Test
  fun `query matches by status filters correctly`() {
    database.matchesQueries.insertMatch(createTestMatch(id = "match1", status = "upcoming"))
    database.matchesQueries.insertMatch(createTestMatch(id = "match2", status = "completed"))
    database.matchesQueries.insertMatch(createTestMatch(id = "match3", status = "upcoming"))

    val upcomingMatches = database.matchesQueries.getMatchesByStatus("upcoming").executeAsList()

    assertEquals(2, upcomingMatches.size)
    assertTrue(upcomingMatches.all { it.status == "upcoming" })
  }

  @Test
  fun `flow emits initial value and updates`() = runTest {
    database.matchesQueries.insertMatch(createTestMatch(id = "match1"))

    val flow = database.matchesQueries.getMatchesWithFavoriteStatus()
      .asFlow()
      .mapToList(DatabaseDispatchers.database)

    val initialMatches = flow.first()
    assertEquals(1, initialMatches.size)
  }

  @Test
  fun `insert match map creates child record`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)

    database.matchesQueries.insertMatchMap(
      match_id = "match1",
      map_name = "Ascent",
      team1_score = 13,
      team2_score = 11,
      duration = "45:30",
      stats_url = "https://example.com/stats",
    )

    val maps = database.matchesQueries.getMatchMaps("match1").executeAsList()

    assertEquals(1, maps.size)
    assertEquals("Ascent", maps[0].map_name)
    assertEquals(13, maps[0].team1_score)
  }

  @Test
  fun `delete match cascades to child tables`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)

    database.matchesQueries.insertMatchMap(
      match_id = "match1",
      map_name = "Ascent",
      team1_score = 13,
      team2_score = 11,
      duration = null,
      stats_url = null,
    )

    database.matchesQueries.deleteMatchById("match1")

    val maps = database.matchesQueries.getMatchMaps("match1").executeAsList()
    assertTrue(maps.isEmpty())
  }

  @Test
  fun `insert match ban creates ban record`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)

    database.matchesQueries.insertMatchBan(
      match_id = "match1",
      ban_type = "map",
      ban_value = "Breeze",
    )

    val bans = database.matchesQueries.getMatchBans("match1").executeAsList()

    assertEquals(1, bans.size)
    assertEquals("Breeze", bans[0].ban_value)
  }

  @Test
  fun `insert match video creates video record`() {
    val match = createTestMatch(id = "match1")
    database.matchesQueries.insertMatch(match)

    database.matchesQueries.insertMatchVideo(
      match_id = "match1",
      video_type = "vod",
      name = "Match VOD",
      url = "https://youtube.com/watch?v=123",
    )

    val videos = database.matchesQueries.getMatchVideos("match1").executeAsList()

    assertEquals(1, videos.size)
    assertEquals("Match VOD", videos[0].name)
  }

  @Test
  fun `transaction rolls back on error`() {
    val match = createTestMatch(id = "match1")

    try {
      database.transaction {
        database.matchesQueries.insertMatch(match)
        // Simulate error
        throw RuntimeException("Test error")
      }
    } catch (e: RuntimeException) {
      // Expected
    }

    val result = database.matchesQueries.getMatchWithFavoriteStatus("match1").executeAsOneOrNull()
    assertEquals(null, result)
  }

  @Test
  fun `transaction commits on success`() {
    val match1 = createTestMatch(id = "match1")
    val match2 = createTestMatch(id = "match2")

    database.transaction {
      database.matchesQueries.insertMatch(match1)
      database.matchesQueries.insertMatch(match2)
    }

    val matches = database.matchesQueries.getMatchesWithFavoriteStatus().executeAsList()
    assertEquals(2, matches.size)
  }

  private fun createTestMatch(
    id: String,
    eventName: String = "Test Event",
    team1Name: String = "Team A",
    team2Name: String = "Team B",
    status: String = "upcoming",
  ): Matches = Matches(
    id = id,
    event_id = "event1",
    event_name = eventName,
    event_logo_url = "https://example.com/logo.png",
    series = "Bo3",
    stage = "Playoffs",
    status = status,
    time = "2025-01-01T12:00:00Z",
    eta = null,
    note = "",
    patch = "8.11",
    team1_id = "team1",
    team1_name = team1Name,
    team1_logo_url = "https://example.com/team1.png",
    team1_score = null,
    team2_id = "team2",
    team2_name = team2Name,
    team2_logo_url = "https://example.com/team2.png",
    team2_score = null,
    map_count = 0,
    last_updated = System.currentTimeMillis(),
  )
}
