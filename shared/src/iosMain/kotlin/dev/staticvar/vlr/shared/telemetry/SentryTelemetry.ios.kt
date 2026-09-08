/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.staticvar.vlr.shared.telemetry

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
  return object : TelemetrySpan {
    override fun finish(status: TelemetrySpanStatus) {
      transaction.finishWithStatus(
        when (status) {
          TelemetrySpanStatus.Ok -> kSentrySpanStatusOk
          TelemetrySpanStatus.Error -> kSentrySpanStatusInternalError
          TelemetrySpanStatus.Cancelled -> kSentrySpanStatusCancelled
        },
      )
    }
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
