/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.telemetry

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

public class RefreshTrace internal constructor(private val span: TelemetrySpan) :
  AbstractCoroutineContextElement(Key) {
  public companion object Key : CoroutineContext.Key<RefreshTrace>

  public fun startChild(operation: String, description: String): TelemetrySpan =
    span.startChild(operation, description)

  public fun <T> traceDatabase(block: () -> T): T {
    val child = startChild("db.transaction", "Persist refreshed data")
    var status = TelemetrySpanStatus.Error
    try {
      return DatabaseTrace.withSpan(child, block).also { status = TelemetrySpanStatus.Ok }
    } catch (cancelled: CancellationException) {
      status = TelemetrySpanStatus.Cancelled
      throw cancelled
    } finally {
      child.finish(status)
    }
  }
}

public suspend fun <T> traceRefresh(
  dispatcher: CoroutineDispatcher,
  operation: String,
  reporter: TelemetryReporter = AppTelemetry,
  block: suspend RefreshTrace.() -> Result<T>,
): Result<T> {
  val span = reporter.startSpan("data.refresh", operation)
  val trace = RefreshTrace(span)
  var status = TelemetrySpanStatus.Error
  try {
    return withContext(dispatcher + trace) {
      val result = trace.block()
      currentCoroutineContext().ensureActive()
      if (result.exceptionOrNull() is CancellationException) throw result.exceptionOrNull()!!
      status = if (result.isSuccess) TelemetrySpanStatus.Ok else TelemetrySpanStatus.Error
      result
    }
  } catch (cancelled: CancellationException) {
    status = TelemetrySpanStatus.Cancelled
    throw cancelled
  } finally {
    span.finish(status)
  }
}
