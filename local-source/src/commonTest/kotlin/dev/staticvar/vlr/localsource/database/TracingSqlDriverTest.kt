/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import dev.staticvar.vlr.core.telemetry.DatabaseTrace
import dev.staticvar.vlr.core.telemetry.TelemetrySpan
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TracingSqlDriverTest {
  @Test
  fun queryCoversCursorMappingAndNeverRecordsSqlOrValues() {
    val parent = Span()
    val delegate = Driver()
    val driver = TracingSqlDriver(delegate)
    DatabaseTrace.withSpan(parent) {
      val result = driver.executeQuery(123, "SELECT 'private-value'", { cursor ->
        assertNull(parent.children.single().status)
        QueryResult.Value(cursor.getString(0))
      }, 0, null)
      assertEquals("private-value", result.value)
    }
    assertEquals(listOf("db.sql.query SQLDelight read 123"), parent.descriptions)
    assertEquals(TelemetrySpanStatus.Ok, parent.children.single().status)
    assertFalse(parent.descriptions.any { "private" in it })
    driver.execute(null, "untraced", 0, null)
    assertEquals(1, parent.children.size)
    assertEquals("untraced", delegate.lastSql)
  }

  @Test
  fun writeFailureClosesSpanAndRestoresParent() {
    val parent = Span()
    val driver = TracingSqlDriver(Driver().apply { fail = true })
    assertFailsWith<IllegalStateException> {
      DatabaseTrace.withSpan(parent) { driver.execute(null, "private SQL", 0, null) }
    }
    assertEquals(listOf("db.sql.query SQLDelight write uncached"), parent.descriptions)
    assertEquals(TelemetrySpanStatus.Error, parent.children.single().status)
    assertEquals(1, parent.children.single().finishes)
    assertFailsWith<IllegalStateException> { driver.execute(null, "private SQL", 0, null) }
    assertEquals(1, parent.children.size)
  }

  private class Span : TelemetrySpan {
    val descriptions = mutableListOf<String>()
    val children = mutableListOf<Span>()
    var status: TelemetrySpanStatus? = null
    var finishes = 0
    override fun startChild(operation: String, description: String): TelemetrySpan {
      descriptions += "$operation $description"
      return Span().also(children::add)
    }
    override fun finish(status: TelemetrySpanStatus) {
      this.status = status
      finishes++
    }
  }

  private class Driver : SqlDriver {
    var fail = false
    var lastSql: String? = null
    override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<Long> {
      lastSql = sql
      if (fail) error("failed")
      return QueryResult.Value(1L)
    }
    override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> QueryResult<R>, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): QueryResult<R> {
      lastSql = sql
      return mapper(object : SqlCursor {
        override fun next(): QueryResult<Boolean> = QueryResult.Value(true)
        override fun getString(index: Int): String = "private-value"
        override fun getLong(index: Int): Long? = null
        override fun getBytes(index: Int): ByteArray? = null
        override fun getDouble(index: Int): Double? = null
        override fun getBoolean(index: Int): Boolean? = null
      })
    }
    override fun newTransaction(): QueryResult<Transacter.Transaction> = error("Unused")
    override fun currentTransaction(): Transacter.Transaction? = null
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) = Unit
    override fun notifyListeners(vararg queryKeys: String) = Unit
    override fun close() = Unit
  }
}
