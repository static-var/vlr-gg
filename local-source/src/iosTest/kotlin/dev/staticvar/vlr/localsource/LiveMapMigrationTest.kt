/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.Match_current_map
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiveMapMigrationTest {
  @Test
  fun versionTwelveUpgradeRebuildsCurrentMapWithoutLiveFlag() {
    val oldSchema = object : SqlSchema<QueryResult.Value<Unit>> by VlrDatabase.Schema {
      override val version = 12L

      override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        VlrDatabase.Schema.create(driver)
        driver.execute(null, "DROP TABLE match_current_map", 0)
        driver.execute(null, """
          CREATE TABLE match_current_map (
              match_id TEXT NOT NULL PRIMARY KEY,
              name TEXT NOT NULL,
              number INTEGER,
              team1_score INTEGER,
              team2_score INTEGER,
              is_live INTEGER NOT NULL,
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
          team1_id, team1_name, team1_logo_url, team2_id, team2_name, team2_logo_url)
        VALUES ('match', 'Champions', '', 'LIVE', '2099-01-01T12:00:00Z',
          '120', 'Alpha', '', '1034', 'Beta', '')
      """.trimIndent(), 0)
      driver.execute(null, """
        INSERT INTO match_current_map(match_id, name, number, team1_score, team2_score, is_live)
        VALUES ('match', 'Ascent', 2, 7, 5, 1)
      """.trimIndent(), 0)

      VlrDatabase.Schema.migrate(driver, 12, 13)

      val database = VlrDatabase(driver)
      assertEquals("Champions", database.matchesQueries.getMatchWithFavoriteStatus("match").executeAsOne().event_name)
      assertNull(database.matchesQueries.getMatchCurrentMap("match").executeAsOneOrNull())
      database.matchesQueries.insertMatchCurrentMap("match", "Bind", 3, 2, 1)
      assertEquals(
        Match_current_map("match", "Bind", 3, 2, 1),
        database.matchesQueries.getMatchCurrentMap("match").executeAsOne(),
      )
    } finally {
      driver.close()
    }
  }
}
