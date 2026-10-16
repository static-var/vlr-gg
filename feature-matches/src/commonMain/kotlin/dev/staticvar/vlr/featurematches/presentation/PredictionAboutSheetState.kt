/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetrySpan
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus

internal class PredictionAboutSheetState(private val reporter: TelemetryReporter) : RememberObserver {
  var visible by mutableStateOf(false)
    private set

  private var opening: TelemetrySpan? = null

  fun open() {
    if (visible) return
    opening = reporter.startSpan(operation = "ui.sheet.open", description = "prediction_about")
    visible = true
  }

  fun onEnterFinished() {
    if (visible) finishOpening(TelemetrySpanStatus.Ok)
  }

  fun dismiss() {
    visible = false
    cancelOpening()
  }

  fun cancelOpening() = finishOpening(TelemetrySpanStatus.Cancelled)

  override fun onRemembered() = Unit

  override fun onForgotten() = cancelOpening()

  override fun onAbandoned() = cancelOpening()

  private fun finishOpening(status: TelemetrySpanStatus) {
    val span = opening ?: return
    opening = null
    span.finish(status)
  }
}
