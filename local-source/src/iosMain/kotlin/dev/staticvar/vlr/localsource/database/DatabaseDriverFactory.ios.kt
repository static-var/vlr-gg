/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation of database driver factory.
 */
actual class DatabaseDriverFactory {
  actual fun createDriver(): SqlDriver = NativeSqliteDriver(
    schema = VlrDatabase.Schema,
    name = DatabaseConstants.DATABASE_NAME,
  ).also { driver ->
    // Enable foreign key constraints
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
  }
}
