/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Desktop (JVM) implementation of database driver factory.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver {
    val databasePath = File(System.getProperty("user.home"), ".vlr/${DatabaseConstants.DATABASE_NAME}")
    databasePath.parentFile?.mkdirs()

    val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")
    VlrDatabase.Schema.create(driver)

    // Enable foreign key constraints
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)

    return driver
  }
}
