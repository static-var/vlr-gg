package dev.staticvar.vlr.localsource.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of database driver factory.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = VlrDatabase.Schema,
            context = context,
            name = DatabaseConstants.DATABASE_NAME
        ).also { driver ->
            // Enable foreign key constraints
            driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        }
    }
}
