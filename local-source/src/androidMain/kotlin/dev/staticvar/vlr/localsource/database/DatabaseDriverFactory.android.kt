/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of database driver factory.
 */
actual class DatabaseDriverFactory(private val context: Context) {
  actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
    schema = VlrDatabase.Schema,
    context = context,
    name = DatabaseConstants.DATABASE_NAME,
    callback = object : AndroidSqliteDriver.Callback(VlrDatabase.Schema) {
      override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
      }

      override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        RoomToKmpMigration.migrate(AndroidSqliteDriver(db))
      }
    },
  )
}
