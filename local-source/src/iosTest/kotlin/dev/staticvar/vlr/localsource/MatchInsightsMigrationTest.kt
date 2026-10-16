/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchInsightsMigrationTest {
  @Test
  fun versionThirteenUpgradePreservesMatchAndReplacesPredictionStorage() {
    val oldSchema = object : SqlSchema<QueryResult.Value<Unit>> by VlrDatabase.Schema {
      override val version = 13L

      override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        VlrDatabase.Schema.create(driver)
        driver.execute(null, "DROP TABLE match_insights", 0)
        driver.execute(null, """
          CREATE TABLE match_predictions (
            match_id TEXT NOT NULL PRIMARY KEY,
            team_a_id TEXT NOT NULL,
            team_b_id TEXT NOT NULL,
            team_a_probability REAL NOT NULL CHECK (team_a_probability BETWEEN 0 AND 1),
            team_b_probability REAL NOT NULL CHECK (team_b_probability BETWEEN 0 AND 1),
            source TEXT NOT NULL CHECK (source IN ('MODEL', 'ELO')),
            warnings_json TEXT NOT NULL DEFAULT '[]',
            FOREIGN KEY(match_id) REFERENCES matches(id) ON DELETE CASCADE
          )
        """.trimIndent(), 0)
        return QueryResult.Value(Unit)
      }
    }
    val driver = inMemoryDriver(oldSchema)
    try {
      driver.execute(null, "PRAGMA foreign_keys = ON", 0)
      driver.execute(null, """
        INSERT INTO matches(id, event_name, event_logo_url, status, time,
          team1_id, team1_name, team1_logo_url, team2_id, team2_name, team2_logo_url, note, patch)
        VALUES ('match', 'Champions', '', 'UPCOMING', '2099-01-01T12:00:00Z',
          '120', 'Alpha', '', '1034', 'Beta', '', 'Grand final', '12.06')
      """.trimIndent(), 0)
      driver.execute(null, "INSERT INTO favorite_matches(match_id) VALUES ('match')", 0)
      driver.execute(null, "INSERT INTO match_predictions VALUES ('match', '120', '1034', 0.6, 0.4, 'MODEL', '[]')", 0)

      VlrDatabase.Schema.migrate(driver, 13, 14)

      val database = VlrDatabase(driver)
      val match = database.matchesQueries.getMatchWithFavoriteStatus("match").executeAsOne()
      assertEquals("Champions", match.event_name)
      assertEquals("Grand final", match.note)
      assertEquals("12.06", match.patch)
      assertEquals(1L, match.is_direct_favorite)
      assertNull(database.matchInsightsQueries.getMatchInsights("match").executeAsOneOrNull())
      database.matchInsightsQueries.upsertMatchInsights("match", "120", "1034", "{}")
      assertEquals("{}", database.matchInsightsQueries.getMatchInsights("match").executeAsOne().insights_json)
      driver.execute(null, "CREATE TABLE match_predictions (unused TEXT)", 0)

      database.matchesQueries.deleteMatchById("match")

      assertNull(database.matchInsightsQueries.getMatchInsights("match").executeAsOneOrNull())
    } finally {
      driver.close()
    }
  }
}
