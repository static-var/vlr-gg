/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.staticvar.vlr.localsource.database.RoomToKmpMigration
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RoomToKmpMigrationTest {
  @Test
  fun roomNineAndFourteenBecomeTheFreshVersionOneSchema() {
    withDriver { fresh ->
      VlrDatabase.Schema.create(fresh)
      val expected = schema(fresh)
      assertEquals(1L, VlrDatabase.Schema.version)
      listOf(9, 14).forEach { version ->
        withDriver { driver ->
          createRoom(driver, version)
          driver.execute(null, "PRAGMA foreign_keys = ON", 0)
          driver.execute(null, "CREATE TABLE android_metadata (locale TEXT)", 0)
          driver.execute(null, "INSERT INTO android_metadata VALUES ('en_US')", 0)
          driver.execute(null, "CREATE TABLE \"legacy\"\"cache\" (value TEXT)", 0)
          if (version == 14) driver.execute(null, "INSERT INTO TeamFav VALUES ('legacy-team')", 0)

          RoomToKmpMigration.migrate(driver)

          assertEquals(expected, schema(driver).filterNot { it[1] == "android_metadata" })
          assertEquals(listOf(listOf("en_US")), rows(driver, "SELECT locale FROM android_metadata", 1))
          assertTrue(rows(driver, "SELECT team_id FROM favorite_teams", 1).isEmpty())
          // The Android open helper owns the version update, not the reset operation.
          assertEquals(version.toString(), rows(driver, "PRAGMA user_version", 1).single().single())
        }
      }
    }
  }

  @Test
  fun aNonRoomDatabaseIsRejectedWithoutChangingItsData() = withDriver { driver ->
    driver.execute(null, "CREATE TABLE user_data (value TEXT)", 0)
    driver.execute(null, "INSERT INTO user_data VALUES ('keep')", 0)
    val before = schema(driver)

    assertFailsWith<IllegalStateException> { RoomToKmpMigration.migrate(driver) }

    assertEquals(before, schema(driver))
    assertEquals(listOf(listOf("keep")), rows(driver, "SELECT value FROM user_data", 1))
  }

  @Test
  fun failedCreationRollsBackTheLegacyTablesAndDataInTheCallersTransaction() = withDriver { driver ->
    createRoom(driver, 14)
    driver.execute(null, "INSERT INTO TeamFav VALUES ('legacy-team')", 0)
    driver.execute(null, "CREATE VIEW news AS SELECT 'conflict' AS id", 0)
    val before = schema(driver)
    driver.execute(null, "BEGIN TRANSACTION", 0)
    try {
      assertFails { RoomToKmpMigration.migrate(driver) }
    } finally {
      driver.execute(null, "ROLLBACK", 0)
    }

    assertEquals(before, schema(driver))
    assertEquals(listOf(listOf("legacy-team")), rows(driver, "SELECT id FROM TeamFav", 1))
    assertEquals(listOf(listOf("14")), rows(driver, "PRAGMA user_version", 1))
  }

  private fun createRoom(driver: SqlDriver, version: Int) {
    val fixture = checkNotNull(javaClass.getResource("/room/$version.sql")).readText()
    fixture.split(';').map(String::trim).filter(String::isNotEmpty).forEach {
      driver.execute(null, it, 0)
    }
  }

  private fun schema(driver: SqlDriver) = rows(
    driver,
    "SELECT type, name, sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type, name",
    3,
  )

  private fun rows(driver: SqlDriver, sql: String, columns: Int): List<List<String?>> =
    driver.executeQuery(null, sql, { cursor ->
      QueryResult.Value(buildList {
        while (cursor.next().value) add(List(columns) { cursor.getString(it) })
      })
    }, 0).value

  private fun withDriver(block: (SqlDriver) -> Unit) {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    try {
      block(driver)
    } finally {
      driver.close()
    }
  }
}
