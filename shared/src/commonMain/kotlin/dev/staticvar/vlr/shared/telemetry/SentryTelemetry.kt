/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.telemetry

import dev.staticvar.vlr.core.telemetry.AppTelemetry
import dev.staticvar.vlr.core.telemetry.NoOpTelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetryLevel
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetrySpan
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb
import kotlinx.coroutines.CancellationException

fun initializeSentryTelemetry(
  dsn: String,
  environment: String,
  release: String,
  dist: String,
  enabled: Boolean = true,
) {
  if (!enabled || dsn.isBlank()) {
    AppTelemetry.install(NoOpTelemetryReporter)
    return
  }
  val initialized = try {
    initializePlatformSentry(SentryConfiguration(dsn, environment, release, dist))
  } catch (_: Exception) {
    false
  }
  AppTelemetry.install(if (initialized) SentryTelemetryReporter else NoOpTelemetryReporter)
  if (initialized) AppTelemetry.log(TelemetryLevel.Info, "VLR monitoring initialized")
}

internal data class SentryConfiguration(
  val dsn: String,
  val environment: String,
  val release: String,
  val dist: String,
)

internal expect fun initializePlatformSentry(configuration: SentryConfiguration): Boolean

internal expect fun startPlatformSpan(operation: String, description: String): TelemetrySpan

internal expect fun submitPlatformFeedback(message: String): Boolean

internal fun SentryOptions.configureTelemetry(configuration: SentryConfiguration) {
  dsn = configuration.dsn
  environment = configuration.environment
  release = configuration.release
  dist = configuration.dist
  debug = false
  sendDefaultPii = false
  enableAutoSessionTracking = true
  enableAppHangTracking = true
  enableWatchdogTerminationTracking = true
  enableUnhandledCppExceptionMonitoring = false
  enableCaptureFailedRequests = false
  sampleRate = 1.0
  tracesSampleRate = 0.1
  attachScreenshot = false
  attachViewHierarchy = true
  maxBreadcrumbs = 100
  sessionReplay.sessionSampleRate = 0.01
  sessionReplay.onErrorSampleRate = 0.1
  sessionReplay.maskAllText = true
  sessionReplay.maskAllImages = true
  logs.enabled = true
  logs.beforeSend = { log ->
    log.body = sanitizeTelemetryText(log.body)
    log.attributes.keys.toList().forEach { key ->
      if (isSensitiveTelemetryKey(key)) log.attributes.remove(key)
      else log.attributes[key]?.stringOrNull?.let { log.attributes[key] = sanitizeTelemetryText(it) }
    }
    log
  }
  beforeBreadcrumb = ::sanitizeBreadcrumb
  beforeSend = { event ->
    event.message?.let { message ->
      message.message = message.message?.let(::sanitizeTelemetryText)
      message.formatted = message.formatted?.let(::sanitizeTelemetryText)
      message.params = message.params?.map(::sanitizeTelemetryText)
    }
    event.breadcrumbs = event.breadcrumbs.map(::sanitizeBreadcrumb).toMutableList()
    event
  }
}

private object SentryTelemetryReporter : TelemetryReporter {
  override fun captureException(error: Throwable, operation: String) {
    if (error is CancellationException) return
    Sentry.captureException(error) { it.setTag("operation", sanitizeTelemetryText(operation)) }
  }

  override fun breadcrumb(category: String, message: String) {
    Sentry.addBreadcrumb(Breadcrumb(category = category, message = sanitizeTelemetryText(message)))
  }

  override fun log(level: TelemetryLevel, message: String) {
    val safeMessage = sanitizeTelemetryText(message)
    when (level) {
      TelemetryLevel.Debug -> Sentry.addBreadcrumb(Breadcrumb.debug(safeMessage))
      TelemetryLevel.Info -> Sentry.logger.info(safeMessage)
      TelemetryLevel.Warning -> Sentry.logger.warn(safeMessage)
      TelemetryLevel.Error -> Sentry.logger.error(safeMessage)
    }
  }

  override fun startSpan(operation: String, description: String): TelemetrySpan =
    startPlatformSpan(operation, sanitizeTelemetryText(description))

  override fun submitFeedback(message: String): Boolean {
    if (!Sentry.isEnabled() || message.isBlank()) return false
    return submitPlatformFeedback(sanitizeTelemetryText(message))
  }
}

private fun sanitizeBreadcrumb(breadcrumb: Breadcrumb): Breadcrumb = breadcrumb.apply {
  message = message?.let(::sanitizeTelemetryText)
  getData()?.let { data ->
    data.keys.toList().forEach { key ->
      if (isSensitiveTelemetryKey(key)) data.remove(key)
      else (data[key] as? String)?.let { data[key] = sanitizeTelemetryText(it) }
    }
  }
}

internal fun isSensitiveTelemetryKey(key: String): Boolean =
  key.lowercase().let { normalized ->
    listOf("authorization", "cookie", "password", "secret", "token", "api_key", "apikey")
      .any { it in normalized } || normalized in setOf("http.query", "http.fragment", "query_string")
  }

private val telemetryUrl = Regex("https?://[^\\s<>\\\"]+")
private val telemetryUrlCredentials = Regex("(https?://)[^/@]+@")
private val telemetryCredential = Regex(
  "(?i)(authorization|cookie|password|secret|token|api[_-]?key)\\s*[:=]\\s*[^\\s,;]+",
)
private val telemetryBearer = Regex("(?i)Bearer\\s+[^\\s,;]+")
private val telemetryHeader = Regex("(?i)\\b(authorization|cookie|set-cookie)\\s*:\\s*[^\\r\\n]*")

internal fun sanitizeTelemetryText(text: String): String = text
  .replace(telemetryUrl) { match -> match.value.substringBefore('?').substringBefore('#') }
  .replace(telemetryUrlCredentials, "$1[redacted]@")
  .replace(telemetryHeader) { match -> "${match.groupValues[1]}: [redacted]" }
  .replace(telemetryBearer, "Bearer [redacted]")
  .replace(telemetryCredential) { match -> "${match.groupValues[1]}=[redacted]" }
