package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific database driver factory.
 * Each platform provides its own implementation.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
