/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.telemetry

import dev.staticvar.vlr.core.telemetry.TelemetrySpan
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus
import io.sentry.SpanStatus
import io.sentry.SentryLogEventAttributeValue
import io.sentry.android.replay.maskAllImages
import io.sentry.android.replay.maskAllText
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.metrics.SentryMetricsParameters
import io.sentry.protocol.Feedback
import io.sentry.protocol.SentryId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.TimeSource
import io.sentry.Sentry as NativeSentry

internal actual fun initializePlatformSentry(configuration: SentryConfiguration): Boolean {
  Sentry.initWithPlatformOptions { options ->
    options.dsn = configuration.dsn
    options.environment = configuration.environment
    options.release = configuration.release
    options.dist = configuration.dist
    options.isDebug = false
    options.isSendDefaultPii = false
    options.isEnableAutoSessionTracking = true
    options.isAnrEnabled = true
    options.isAttachScreenshot = false
    options.isAttachViewHierarchy = true
    options.tracesSampleRate = 0.1
    options.profilesSampleRate = 0.0
    options.profileSessionSampleRate = 0.0
    options.logs.isEnabled = true
    options.logs.setBeforeSend { log ->
      log.body = sanitizeTelemetryText(log.body)
      log.attributes?.let { attributes ->
        attributes.keys.toList().forEach { key ->
          if (isSensitiveTelemetryKey(key)) attributes.remove(key)
          else (attributes[key]?.value as? String)?.let { value ->
            attributes[key] = SentryLogEventAttributeValue("string", sanitizeTelemetryText(value))
          }
        }
      }
      log
    }
    options.sessionReplay.sessionSampleRate = 0.01
    options.sessionReplay.onErrorSampleRate = 0.1
    options.sessionReplay.maskAllText = true
    options.sessionReplay.maskAllImages = true
    options.setBeforeSend { event, _ ->
      event.request?.let { request ->
        request.headers = emptyMap()
        request.cookies = null
        request.data = null
        request.queryString = null
        request.url = request.url?.let(::sanitizeTelemetryText)
      }
      event.exceptions?.forEach { it.value = it.value?.let(::sanitizeTelemetryText) }
      event.message?.let {
        it.message = it.message?.let(::sanitizeTelemetryText)
        it.formatted = it.formatted?.let(::sanitizeTelemetryText)
      }
      event
    }
    options.setBeforeBreadcrumb { breadcrumb, _ ->
      breadcrumb.message = breadcrumb.message?.let(::sanitizeTelemetryText)
      breadcrumb.data.keys.toList().forEach { key ->
        if (isSensitiveTelemetryKey(key)) breadcrumb.removeData(key)
        else (breadcrumb.getData(key) as? String)?.let { breadcrumb.setData(key, sanitizeTelemetryText(it)) }
      }
      breadcrumb
    }
  }
  return Sentry.isEnabled()
}

internal actual fun startPlatformSpan(operation: String, description: String): TelemetrySpan {
  val started = TimeSource.Monotonic.markNow()
  val finished = AtomicBoolean(false)
  val transaction = NativeSentry.startTransaction(description, operation)
  return object : TelemetrySpan {
    override fun finish(status: TelemetrySpanStatus) {
      if (!finished.compareAndSet(false, true)) return
      transaction.finish(
        when (status) {
          TelemetrySpanStatus.Ok -> SpanStatus.OK
          TelemetrySpanStatus.Error -> SpanStatus.INTERNAL_ERROR
          TelemetrySpanStatus.Cancelled -> SpanStatus.CANCELLED
        },
      )
      val parameters = SentryMetricsParameters.create(
        mapOf("operation" to operation, "status" to status.name.lowercase()),
      )
      NativeSentry.metrics().count("vlr.operation.count", 1.0, "operation", parameters)
      NativeSentry.metrics().distribution(
        "vlr.operation.duration",
        started.elapsedNow().inWholeMicroseconds / 1000.0,
        "millisecond",
        parameters,
      )
    }
  }
}

internal actual fun submitPlatformFeedback(message: String): Boolean =
  NativeSentry.captureFeedback(Feedback(message)) != SentryId.EMPTY_ID
