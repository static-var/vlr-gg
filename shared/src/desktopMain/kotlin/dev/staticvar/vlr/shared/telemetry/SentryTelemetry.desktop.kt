/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.telemetry

import dev.staticvar.vlr.core.telemetry.NoOpTelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetrySpan

internal actual fun initializePlatformSentry(configuration: SentryConfiguration): Boolean = false

internal actual fun startPlatformSpan(operation: String, description: String): TelemetrySpan =
  NoOpTelemetryReporter.startSpan(operation, description)

internal actual fun submitPlatformFeedback(message: String): Boolean = false
