/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import dev.staticvar.vlr.core.telemetry.DatabaseTrace

/** The mobile drivers execute and consume their cursors synchronously. */
internal class TracingSqlDriver(private val driver: SqlDriver) : SqlDriver by driver {
  override fun execute(
    identifier: Int?,
    sql: String,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<Long> = DatabaseTrace.query(identifier, read = false) {
    driver.execute(identifier, sql, parameters, binders)
  }

  override fun <R> executeQuery(
    identifier: Int?,
    sql: String,
    mapper: (SqlCursor) -> QueryResult<R>,
    parameters: Int,
    binders: (SqlPreparedStatement.() -> Unit)?,
  ): QueryResult<R> = DatabaseTrace.query(identifier, read = true) {
    driver.executeQuery(identifier, sql, mapper, parameters, binders)
  }
}
