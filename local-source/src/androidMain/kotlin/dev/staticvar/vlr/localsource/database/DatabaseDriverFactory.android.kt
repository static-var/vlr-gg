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
    schema = MigratingDatabaseSchema,
    context = context,
    name = DatabaseConstants.DATABASE_NAME,
    callback = object : AndroidSqliteDriver.Callback(MigratingDatabaseSchema) {
      override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
      }

      override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (db.hasRoomSchema()) {
          RoomToKmpMigration.migrate(AndroidSqliteDriver(db))
        } else {
          super.onUpgrade(db, oldVersion, newVersion)
        }
      }

      override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        if (db.hasRoomSchema()) {
          db.beginTransaction()
          try {
            RoomToKmpMigration.migrate(AndroidSqliteDriver(db))
            db.setTransactionSuccessful()
          } finally {
            db.endTransaction()
          }
        }
      }

      override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        RoomToKmpMigration.migrate(AndroidSqliteDriver(db))
      }
    },
  ).let(::TracingSqlDriver)
}

private fun SupportSQLiteDatabase.hasRoomSchema(): Boolean = query(
  "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'room_master_table'",
).use { it.moveToFirst() }
