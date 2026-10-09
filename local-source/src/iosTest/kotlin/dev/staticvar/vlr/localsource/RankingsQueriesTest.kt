/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.MigratingDatabaseSchema
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RankingsQueriesTest {
  private lateinit var driver: SqlDriver
  private lateinit var database: VlrDatabase

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)

    database = VlrDatabase(driver)
  }

  @AfterTest
  fun teardown() { driver.close() }

  @Test
  fun rankings_are_read_in_position_order() {
    insertQueryRanking("team-b", position = 1, rank = 1, region = "emea")
    insertQueryRanking("team-z", position = 0, rank = 3, region = "americas")
    insertQueryRanking("team-a", position = 2, rank = 1)
    val rows = database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus("global").executeAsList()
    assertEquals(listOf("team-z", "team-b", "team-a"), rows.map { it.team_id })
    assertEquals(listOf(3L, 1L, 1L), rows.map { it.rank })
    assertEquals(listOf("americas", "emea", null), rows.map { it.region })
  }

  @Test
  fun replace_ranking_updates() {
    insertQueryRanking("team1", position = 1, rank = 2, elo = 1700.0)
    insertQueryRanking("team1", position = 0, rank = 1, elo = 1795.5)
    val rows = database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus("global").executeAsList()
    assertEquals(1, rows.size)
    assertEquals(1L, rows.first().rank)
    assertEquals(1795.5, rows.first().elo)
  }

  @Test
  fun deleting_query_rankings_preserves_other_snapshots() {
    insertQueryRanking("team1", position = 0, rank = 1)
    insertQueryRanking("team2", position = 0, rank = 1, queryKey = "regional")
    database.rankingsQueries.deleteRankingQueryTeams("global")
    assertTrue(database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus("global").executeAsList().isEmpty())
    assertEquals("team2", database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus("regional").executeAsOne().team_id)
  }

  @Test
  fun version_nine_upgrade_removes_legacy_cache_and_preserves_query_snapshots_and_favorites() {
    insertQueryRanking("team1", position = 0, rank = 1)
    database.teamsQueries.addFavoriteTeam("team1")
    driver.execute(null, "CREATE TABLE rankings (team_id TEXT NOT NULL PRIMARY KEY)", 0)
    driver.execute(null, "INSERT INTO rankings VALUES ('legacy-team')", 0)

    MigratingDatabaseSchema.migrate(driver, 9, 10)

    val legacyTableCount = driver.executeQuery(
      identifier = null,
      sql = "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'rankings'",
      mapper = { cursor ->
        cursor.next()
        QueryResult.Value(cursor.getLong(0))
      },
      parameters = 0,
    ).value
    assertEquals(0L, legacyTableCount)
    val snapshot = database.rankingsQueries.getRankingQueryTeamsWithFavoriteStatus("global").executeAsOne()
    assertEquals("team1", snapshot.team_id)
    assertEquals(1L, snapshot.is_favorite)
    assertEquals(1L, database.teamsQueries.isFavoriteTeam("team1").executeAsOne())
  }

  @Test
  fun standings_by_year_and_region() {
    database.rankingsQueries.insertStanding("team1", 2025, "Champ", "NA", 1, "150", 0)
    database.rankingsQueries.insertStanding("team2", 2025, "Champ", "NA", 2, "120", 0)
    database.rankingsQueries.insertStanding("team3", 2025, "Challengers", "EU", 1, "110", 0)
    val na = database.rankingsQueries.getStandingsByYearAndRegion(2025, "NA").executeAsList()
    assertEquals(2, na.size)
    assertEquals("team1", na.first().team_id)
  }

  @Test
  fun standings_by_circuit() {
    database.rankingsQueries.insertStanding("team1", 2025, "Champ", "NA", 1, "150", 0)
    database.rankingsQueries.insertStanding("team2", 2024, "Champ", "EU", 2, "120", 0)
    val champ = database.rankingsQueries.getStandingsByCircuit("Champ").executeAsList()
    assertEquals(2, champ.size)
    assertTrue(champ.first().year >= champ.last().year)
  }

  @Test
  fun delete_standings_by_year_and_region() {
    database.rankingsQueries.insertStanding("team1", 2025, "Champ", "NA", 1, "150", 0)
    database.rankingsQueries.deleteStandingsByYearAndRegion(2025, "NA")
    assertTrue(database.rankingsQueries.getStandingsByYearAndRegion(2025, "NA").executeAsList().isEmpty())
  }

  private fun insertQueryRanking(
    teamId: String,
    position: Long,
    rank: Long,
    elo: Double = 1500.0,
    region: String? = null,
    queryKey: String = "global",
  ) {
    database.rankingsQueries.insertRankingQueryTeam(
      query_key = queryKey, team_id = teamId, team_name = "Team $teamId", team_logo = "", country = "",
      rank = rank, overall_rank = rank, position = position, elo = elo, map_elo = 1400.0,
      matches_played = 10, match_wins = 7, match_losses = 3, win_rate = 0.7,
      region = region, last_updated = 123,
    )
  }
}
