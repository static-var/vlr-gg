/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver

/** Replaces the Room schema inside the Android database helper's migration transaction. */
internal object RoomToKmpMigration {
  fun migrate(driver: SqlDriver) {
    val tables = driver.executeQuery(
      null,
      "SELECT name FROM sqlite_master WHERE type = 'table'",
      { cursor ->
        QueryResult.Value(buildList {
          while (cursor.next().value) add(checkNotNull(cursor.getString(0)))
        })
      },
      0,
    ).value
    check("room_master_table" in tables) { "Only a legacy Room database can be reset to the KMP schema" }
    tables.filterNot { it.startsWith("sqlite_") || it == "android_metadata" }.forEach { table ->
      val quotedName = table.replace("\"", "\"\"")
      driver.execute(null, "DROP TABLE \"$quotedName\"", 0)
    }
    VlrDatabase.Schema.create(driver)
  }
}
