/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.android.notifications

import android.os.SystemClock
import android.util.Log
import dev.staticvar.vlr.android.BuildConfig
import dev.staticvar.vlr.core.telemetry.AppTelemetry
import kotlinx.coroutines.CancellationException

/** Bounds notification diagnostics and excludes payloads, credentials, and exception messages. */
internal object LiveNotificationDiagnostics {
  private val lastReported = mutableMapOf<String, Long>()

  fun skipped(reason: String, matchId: String? = null) {
    if (!BuildConfig.DEBUG || !shouldReport("skip:$reason")) return
    val safeMatchId = matchId?.takeIf { it.length in 1..10 && it.all { char -> char in '0'..'9' } }
    Log.d("LiveMatchNotifications", "skipped=$reason, matchId=${safeMatchId ?: "unknown"}")
  }

  /** Captures a sanitized stack at most once per operation per minute; cancellation propagates. */
  fun failed(operation: String, error: Exception) {
    if (error is CancellationException) throw error
    if (!shouldReport("failure:$operation")) return
    val message = "Live notification $operation failed (${error.javaClass.simpleName})"
    val sanitized = IllegalStateException(message)
      .apply { stackTrace = error.stackTrace }
    AppTelemetry.breadcrumb("live_notifications", "operation=$operation failed")
    AppTelemetry.captureException(sanitized, "live_notifications.$operation")
    if (BuildConfig.DEBUG) Log.w("LiveMatchNotifications", message)
  }

  private fun shouldReport(key: String): Boolean = synchronized(lastReported) {
    val now = SystemClock.elapsedRealtime()
    lastReported.entries.removeAll { now - it.value >= 60_000L }
    if (key in lastReported || lastReported.size >= 32) return@synchronized false
    lastReported[key] = now
    true
  }
}
