/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.telemetry

import kotlinx.coroutines.CancellationException

/** Bound only around synchronous SQLDelight work; never across a suspension. */
public object DatabaseTrace {
  public fun <T> withSpan(span: TelemetrySpan, block: () -> T): T {
    val previous = databaseTraceSpan
    databaseTraceSpan = span
    try {
      return block()
    } finally {
      databaseTraceSpan = previous
    }
  }

  public fun <T> query(identifier: Int?, read: Boolean, block: () -> T): T {
    val parent = databaseTraceSpan ?: return block()
    val kind = if (read) "read" else "write"
    val span = parent.startChild("db.sql.query", "SQLDelight $kind ${identifier ?: "uncached"}")
    var status = TelemetrySpanStatus.Error
    try {
      return block().also { status = TelemetrySpanStatus.Ok }
    } catch (cancelled: CancellationException) {
      status = TelemetrySpanStatus.Cancelled
      throw cancelled
    } finally {
      span.finish(status)
    }
  }
}

internal expect var databaseTraceSpan: TelemetrySpan?
