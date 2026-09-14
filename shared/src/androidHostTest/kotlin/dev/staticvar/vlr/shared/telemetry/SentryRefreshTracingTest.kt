package dev.staticvar.vlr.shared.telemetry

import dev.staticvar.vlr.core.telemetry.DatabaseTrace
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEnvelope
import io.sentry.SentryOptions
import io.sentry.SpanStatus
import io.sentry.protocol.SentryTransaction
import io.sentry.transport.ITransport
import io.sentry.transport.RateLimiter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class SentryRefreshTracingTest {
  @Test
  fun nativeEnvelopePreservesRefreshHttpDatabaseAndQueryParentage() = withCapturedTransactions { captured ->
    val refresh = startPlatformSpan("data.refresh", "matches.refresh")
    val http = refresh.startChild("http.client", "GET /api/v1/matches")
    http.finish(TelemetrySpanStatus.Ok)
    val database = refresh.startChild("db.transaction", "matches.persist")
    val query = database.startChild("db.sql.query", "SELECT statement=42")
    query.finish(TelemetrySpanStatus.Error)
    query.finish(TelemetrySpanStatus.Ok)
    database.finish(TelemetrySpanStatus.Error)
    refresh.finish(TelemetrySpanStatus.Error)
    refresh.finish(TelemetrySpanStatus.Ok)

    val transaction = captured.single()
    val root = transaction.contexts.trace!!
    val httpSpan = transaction.spans.single { it.op == "http.client" }
    val databaseSpan = transaction.spans.single { it.op == "db.transaction" }
    val querySpan = transaction.spans.single { it.op == "db.sql.query" }
    assertEquals(root.spanId, httpSpan.parentSpanId)
    assertEquals(root.spanId, databaseSpan.parentSpanId)
    assertEquals(databaseSpan.spanId, querySpan.parentSpanId)
    assertEquals(SpanStatus.INTERNAL_ERROR, querySpan.status)
    assertEquals(SpanStatus.INTERNAL_ERROR, root.status)
    assertEquals(3, transaction.spans.size)
    assertNull(Sentry.getSpan())
  }

  @Test
  fun concurrentNativeRefreshesKeepSeparateTraceIdentities() = withCapturedTransactions { captured ->
    val executor = Executors.newFixedThreadPool(2)
    val bothStarted = CyclicBarrier(2)
    try {
      val futures = listOf("matches", "events").map { resource ->
        executor.submit {
          val refresh = startPlatformSpan("data.refresh", "$resource.refresh")
          val database = refresh.startChild("db.transaction", "$resource.persist")
          DatabaseTrace.withSpan(database) {
            bothStarted.await(5, TimeUnit.SECONDS)
            DatabaseTrace.query(42, read = true) { Unit }
          }
          DatabaseTrace.query(43, read = true) { Unit }
          database.finish(TelemetrySpanStatus.Ok)
          refresh.finish(TelemetrySpanStatus.Cancelled)
        }
      }
      futures.forEach { it.get(10, TimeUnit.SECONDS) }
    } finally {
      executor.shutdownNow()
    }
    assertEquals(2, captured.size)
    assertNotEquals(captured[0].contexts.trace!!.traceId, captured[1].contexts.trace!!.traceId)
    captured.forEach { transaction ->
      val root = transaction.contexts.trace!!
      assertEquals(SpanStatus.CANCELLED, root.status)
      val database = transaction.spans.single { it.op == "db.transaction" }
      assertEquals(2, transaction.spans.size)
      val query = transaction.spans.single { it.op == "db.sql.query" }
      assertEquals(root.spanId, database.parentSpanId)
      assertEquals(database.spanId, query.parentSpanId)
      transaction.spans.forEach { assertEquals(root.traceId, it.traceId) }
    }
  }

  private fun withCapturedTransactions(block: (List<SentryTransaction>) -> Unit) {
    val captured = CopyOnWriteArrayList<SentryTransaction>()
    val options = SentryOptions().apply {
      dsn = "https://public@example.invalid/1"
      tracesSampleRate = 1.0
      integrations.clear()
      setEnableShutdownHook(false)
      setTransportFactory(io.sentry.ITransportFactory { configuration, _ ->
        object : ITransport {
          override fun send(envelope: SentryEnvelope, hint: Hint) {
            envelope.items.mapNotNull { it.getTransaction(configuration.serializer) }.forEach(captured::add)
          }
          override fun flush(timeoutMillis: Long) = Unit
          override fun getRateLimiter(): RateLimiter? = null
          override fun close() = Unit
          override fun close(isRestarting: Boolean) = Unit
        }
      })
    }
    Sentry.init(options)
    try {
      block(captured)
    } finally {
      Sentry.close()
    }
  }
}
