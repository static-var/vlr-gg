/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

/**
 * Desktop (JVM) implementation of database driver factory.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    val databasePath = File(System.getProperty("user.home"), ".vlr/${DatabaseConstants.DATABASE_NAME}")
    databasePath.parentFile?.mkdirs()

    return JdbcSqliteDriver(
      url = "jdbc:sqlite:${databasePath.absolutePath}",
      properties = Properties().apply { setProperty("foreign_keys", "true") },
      schema = VlrDatabase.Schema,
    )
  }
}
