/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.telemetry

private val currentDatabaseSpan = ThreadLocal<TelemetrySpan?>()

internal actual var databaseTraceSpan: TelemetrySpan?
  get() = currentDatabaseSpan.get()
  set(value) { currentDatabaseSpan.set(value) }
