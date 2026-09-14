/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.staticvar.vlr.shared.telemetry

import cocoapods.Sentry.SentrySpanProtocol
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import cocoapods.Sentry.SentrySDK
import cocoapods.Sentry.SentryFeedback
import cocoapods.Sentry.SentryFeedbackSourceCustom
import cocoapods.Sentry.SentrySpanStatus.kSentrySpanStatusCancelled
import cocoapods.Sentry.SentrySpanStatus.kSentrySpanStatusInternalError
import cocoapods.Sentry.SentrySpanStatus.kSentrySpanStatusOk
import dev.staticvar.vlr.core.telemetry.TelemetrySpan
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus
import io.sentry.kotlin.multiplatform.Sentry

internal actual fun initializePlatformSentry(configuration: SentryConfiguration): Boolean {
  Sentry.init { it.configureTelemetry(configuration) }
  return Sentry.isEnabled()
}

internal actual fun startPlatformSpan(operation: String, description: String): TelemetrySpan {
  val transaction = SentrySDK.startTransactionWithName(description, operation)
  return IosTelemetrySpan(transaction)
}

@OptIn(ExperimentalAtomicApi::class)
private class IosTelemetrySpan(private val span: SentrySpanProtocol) : TelemetrySpan {
  private val finished = AtomicBoolean(false)

  override fun startChild(operation: String, description: String): TelemetrySpan =
    IosTelemetrySpan(span.startChildWithOperation(operation, sanitizeTelemetryText(description)))

  override fun finish(status: TelemetrySpanStatus) {
    if (!finished.compareAndSet(false, true)) return
    span.finishWithStatus(
      when (status) {
        TelemetrySpanStatus.Ok -> kSentrySpanStatusOk
        TelemetrySpanStatus.Error -> kSentrySpanStatusInternalError
        TelemetrySpanStatus.Cancelled -> kSentrySpanStatusCancelled
      },
    )
  }
}

internal actual fun submitPlatformFeedback(message: String): Boolean {
  SentrySDK.captureFeedback(
    SentryFeedback(
      message = message,
      name = null,
      email = null,
      source = SentryFeedbackSourceCustom,
      associatedEventId = null,
      attachments = null,
    ),
  )
  return true
}
