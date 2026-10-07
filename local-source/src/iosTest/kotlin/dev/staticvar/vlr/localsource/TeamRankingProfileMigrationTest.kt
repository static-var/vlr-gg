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

class TeamRankingProfileMigrationTest {
  @Test
  fun migration_adds_nullable_profile_without_losing_existing_team_metadata() {
    val oldSchema = object : SqlSchema<QueryResult.Value<Unit>> by VlrDatabase.Schema {
      override val version = 10L

      override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
        driver.execute(
          null,
          """
          CREATE TABLE teams (
            id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, tag TEXT NOT NULL DEFAULT '',
            logo_url TEXT NOT NULL, region TEXT, country TEXT NOT NULL, roster_url TEXT,
            earnings TEXT, rank INTEGER NOT NULL DEFAULT 0, website TEXT, twitter TEXT,
            last_updated INTEGER NOT NULL DEFAULT 0
          )
          """.trimIndent(),
          0,
        )
        return QueryResult.Value(Unit)
      }
    }
    val driver = inMemoryDriver(oldSchema)
    try {
      driver.execute(
        null,
        """
        INSERT INTO teams(id, name, tag, logo_url, region, country, rank, website, last_updated)
        VALUES ('2593', 'FNATIC', 'FNC', 'logo.png', 'EU', 'GB', 4, 'https://fnatic.com', 123)
        """.trimIndent(),
        0,
      )

      VlrDatabase.Schema.migrate(driver, 10, VlrDatabase.Schema.version)

      val query = "SELECT name, website, last_updated, ranking_profile FROM teams WHERE id = '2593'"
      val team = driver.executeQuery(null, query, { cursor ->
        check(cursor.next().value)
        QueryResult.Value(listOf(cursor.getString(0), cursor.getString(1), cursor.getLong(2), cursor.getString(3)))
      }, 0).value
      assertEquals(listOf("FNATIC", "https://fnatic.com", 123L), team.take(3))
      assertNull(team.last())
    } finally {
      driver.close()
    }
  }
}
