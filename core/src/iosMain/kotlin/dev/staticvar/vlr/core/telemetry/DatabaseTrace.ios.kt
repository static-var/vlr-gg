/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.telemetry

@kotlin.native.concurrent.ThreadLocal
private var currentDatabaseSpan: TelemetrySpan? = null

internal actual var databaseTraceSpan: TelemetrySpan?
  get() = currentDatabaseSpan
  set(value) { currentDatabaseSpan = value }
