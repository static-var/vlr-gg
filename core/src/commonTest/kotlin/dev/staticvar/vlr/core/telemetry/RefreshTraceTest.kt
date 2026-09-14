/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class RefreshTraceTest {
  @Test
  fun overlappingRefreshesKeepTheirParentsAcrossDispatcherChanges() = runTest {
    val reporter = Recorder()
    val dispatcher = StandardTestDispatcher(testScheduler)
    listOf("matches", "news").map { name ->
      async {
        traceRefresh(dispatcher, name, reporter) {
          val parent = this
          yield()
          withContext(StandardTestDispatcher(testScheduler)) {
            assertSame(parent, currentCoroutineContext()[RefreshTrace])
            startChild("http.client", name).finish(TelemetrySpanStatus.Ok)
          }
          traceDatabase { DatabaseTrace.query(42, true) { assertNull(reporter.roots[name]?.status) } }
          Result.success(Unit)
        }
      }
    }.awaitAll()
    reporter.roots.values.forEach { root ->
      assertEquals(TelemetrySpanStatus.Ok, root.status)
      assertEquals(listOf("http.client", "db.transaction"), root.children.map { it.operation })
      assertEquals("db.sql.query", root.children.last().children.single().operation)
      assertEquals(1, root.finishes)
    }
    assertNull(currentCoroutineContext()[RefreshTrace])
    assertNull(databaseTraceSpan)
  }

  @Test
  fun failuresAndReturnedCancellationFinishAndRestoreContext() = runTest {
    val reporter = Recorder()
    val dispatcher = StandardTestDispatcher(testScheduler)
    val failure = IllegalStateException("failed")
    val result = traceRefresh(dispatcher, "failure", reporter) {
      runCatching { traceDatabase { DatabaseTrace.query(7, false) { throw failure } } }
    }
    assertSame(failure, result.exceptionOrNull())
    val failed = reporter.roots.getValue("failure")
    assertEquals(TelemetrySpanStatus.Error, failed.status)
    assertEquals(TelemetrySpanStatus.Error, failed.children.single().status)
    assertEquals(TelemetrySpanStatus.Error, failed.children.single().children.single().status)
    assertFailsWith<CancellationException> {
      traceRefresh(dispatcher, "cancelled", reporter) { Result.failure<Unit>(CancellationException()) }
    }
    assertEquals(TelemetrySpanStatus.Cancelled, reporter.roots.getValue("cancelled").status)
    assertNull(databaseTraceSpan)
  }

  @Test
  fun nestedDatabaseScopesRestoreOuterParentEvenOnFailure() {
    val outer = Span("outer")
    val inner = Span("inner")
    DatabaseTrace.withSpan(outer) {
      assertFailsWith<IllegalStateException> {
        DatabaseTrace.withSpan(inner) { error("fail") }
      }
      DatabaseTrace.query(5, true) { Unit }
    }
    DatabaseTrace.query(6, true) { Unit }
    assertEquals(1, outer.children.size)
    assertEquals(0, inner.children.size)
    assertNull(databaseTraceSpan)
  }

  private class Recorder : TelemetryReporter by NoOpTelemetryReporter {
    val roots = mutableMapOf<String, Span>()
    override fun startSpan(operation: String, description: String): TelemetrySpan =
      Span(operation).also { roots[description] = it }
  }

  private class Span(val operation: String) : TelemetrySpan {
    val children = mutableListOf<Span>()
    var status: TelemetrySpanStatus? = null
    var finishes = 0
    override fun startChild(operation: String, description: String): TelemetrySpan =
      Span(operation).also(children::add)
    override fun finish(status: TelemetrySpanStatus) {
      this.status = status
      finishes++
    }
  }
}
