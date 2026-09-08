/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.core.telemetry

import kotlin.concurrent.Volatile
import kotlinx.coroutines.CancellationException

public enum class TelemetryLevel { Debug, Info, Warning, Error }

public enum class TelemetrySpanStatus { Ok, Error, Cancelled }

public interface TelemetrySpan {
  public fun finish(status: TelemetrySpanStatus)
}

public interface TelemetryReporter {
  public fun captureException(error: Throwable, operation: String)
  public fun breadcrumb(category: String, message: String)
  public fun log(level: TelemetryLevel, message: String)
  public fun startSpan(operation: String, description: String): TelemetrySpan
  public fun submitFeedback(message: String): Boolean = false
}

public object NoOpTelemetrySpan : TelemetrySpan {
  override fun finish(status: TelemetrySpanStatus): Unit = Unit
}

public object NoOpTelemetryReporter : TelemetryReporter {
  override fun captureException(error: Throwable, operation: String): Unit = Unit
  override fun breadcrumb(category: String, message: String): Unit = Unit
  override fun log(level: TelemetryLevel, message: String): Unit = Unit
  override fun startSpan(operation: String, description: String): TelemetrySpan = NoOpTelemetrySpan
}

/** The mobile entry point installs its reporter before starting application work. */
public object AppTelemetry : TelemetryReporter {
  @Volatile
  private var reporter: TelemetryReporter = NoOpTelemetryReporter

  public fun install(reporter: TelemetryReporter) {
    require(reporter !== this) { "AppTelemetry cannot report to itself" }
    this.reporter = reporter
  }

  override fun captureException(error: Throwable, operation: String) {
    reporter.captureExceptionSafely(error, operation)
  }

  override fun breadcrumb(category: String, message: String) {
    telemetryOrNull { reporter.breadcrumb(category, message) }
  }

  override fun log(level: TelemetryLevel, message: String) {
    reporter.logSafely(level, message)
  }

  override fun submitFeedback(message: String): Boolean =
    telemetryOrNull { reporter.submitFeedback(message) } ?: false

  override fun startSpan(operation: String, description: String): TelemetrySpan {
    val span = telemetryOrNull { reporter.startSpan(operation, description) } ?: return NoOpTelemetrySpan
    return object : TelemetrySpan {
      override fun finish(status: TelemetrySpanStatus) {
        telemetryOrNull { span.finish(status) }
      }
    }
  }
}

internal fun TelemetryReporter.captureExceptionSafely(error: Throwable, operation: String) {
  if (error is CancellationException) return
  telemetryOrNull { captureException(error, operation) }
}

internal fun TelemetryReporter.logSafely(level: TelemetryLevel, message: String) {
  telemetryOrNull { log(level, message) }
}

private inline fun <T> telemetryOrNull(action: () -> T): T? = try {
  action()
} catch (_: Exception) {
  null
}
