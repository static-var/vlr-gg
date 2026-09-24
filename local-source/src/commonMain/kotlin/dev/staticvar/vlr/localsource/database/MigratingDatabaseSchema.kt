/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/** Upgrades the unversioned KMP schemas before applying SQLDelight's versioned migrations. */
object MigratingDatabaseSchema : SqlSchema<QueryResult.Value<Unit>> by VlrDatabase.Schema {
  override fun migrate(
    driver: SqlDriver,
    oldVersion: Long,
    newVersion: Long,
    vararg callbacks: AfterVersion,
  ): QueryResult.Value<Unit> {
    if (oldVersion == 1L && newVersion > 1L) migrateVersionOneColumns(driver)
    return VlrDatabase.Schema.migrate(driver, oldVersion, newVersion, *callbacks)
  }

  /** Version 1 builds added columns without changing their version; inspect each before adding it. */
  private fun migrateVersionOneColumns(driver: SqlDriver) {
    addMissingColumns(driver, "event_matches", mapOf(
      "time" to "TEXT NOT NULL DEFAULT ''",
      "date" to "TEXT NOT NULL DEFAULT ''",
      "status" to "TEXT NOT NULL DEFAULT ''",
      "team1_name" to "TEXT NOT NULL DEFAULT ''",
      "team1_region" to "TEXT NOT NULL DEFAULT ''",
      "team2_name" to "TEXT NOT NULL DEFAULT ''",
      "team2_region" to "TEXT NOT NULL DEFAULT ''",
      "eta" to "TEXT",
      "team1_score" to "INTEGER",
      "team2_score" to "INTEGER",
    ))
    for (table in listOf("rankings", "standings")) {
      addMissingColumns(driver, table, mapOf(
        "team_name" to "TEXT NOT NULL DEFAULT ''",
        "team_logo" to "TEXT NOT NULL DEFAULT ''",
        "country" to "TEXT NOT NULL DEFAULT ''",
      ))
    }
    addMissingColumns(driver, "team_roster", mapOf(
      "player_alias" to "TEXT NOT NULL DEFAULT ''",
      "player_image_url" to "TEXT NOT NULL DEFAULT ''",
    ))
    addMissingColumns(driver, "team_completed_matches", mapOf("stage" to "TEXT NOT NULL DEFAULT ''"))
    addMissingColumns(driver, "team_upcoming_matches", mapOf(
      "stage" to "TEXT NOT NULL DEFAULT ''",
      "eta" to "TEXT",
    ))
    addMissingColumns(driver, "news", mapOf("list_position" to "INTEGER"))
    addMissingColumns(driver, "event_overview", mapOf("list_position" to "INTEGER NOT NULL DEFAULT 0"))
  }

  private fun addMissingColumns(driver: SqlDriver, table: String, definitions: Map<String, String>) {
    val columns = driver.executeQuery(null, "PRAGMA table_info($table)", { cursor ->
      QueryResult.Value(buildSet {
        while (cursor.next().value) add(checkNotNull(cursor.getString(1)))
      })
    }, 0).value
    // Tables absent from older builds are created by 1.sqm.
    if (columns.isEmpty()) return
    definitions.filterKeys { it !in columns }.forEach { (column, definition) ->
      driver.execute(null, "ALTER TABLE $table ADD COLUMN $column $definition", 0)
    }
  }
}
