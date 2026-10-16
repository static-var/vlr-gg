/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.presentation

import dev.staticvar.vlr.core.telemetry.NoOpTelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetryReporter
import dev.staticvar.vlr.core.telemetry.TelemetrySpan
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus.Cancelled
import dev.staticvar.vlr.core.telemetry.TelemetrySpanStatus.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PredictionAboutSheetStateTest {
  private val reporter = RecordingReporter()
  private val sheet = PredictionAboutSheetState(reporter)

  @Test
  fun duplicateOpenStartsOneLabelledSpan() {
    sheet.open()
    sheet.open()

    assertTrue(sheet.visible)
    assertEquals(listOf("ui.sheet.open" to "prediction_about"), reporter.started)
  }

  @Test
  fun finishedEnterCompletesTheOpeningOnce() {
    sheet.open()
    sheet.onEnterFinished()
    sheet.onEnterFinished()
    sheet.dismiss()
    sheet.onForgotten()

    assertFalse(sheet.visible)
    assertEquals(listOf(listOf(Ok)), reporter.finishes)
  }

  @Test
  fun earlyDismissCancelsAndTheNextOpeningGetsAFreshSpan() {
    sheet.open()
    sheet.dismiss()
    sheet.onEnterFinished()
    sheet.open()
    sheet.onEnterFinished()

    assertEquals(2, reporter.started.size)
    assertEquals(listOf(listOf(Cancelled), listOf(Ok)), reporter.finishes)
  }

  @Test
  fun forgettingAPendingOpeningCancelsItOnce() {
    sheet.open()
    sheet.onForgotten()
    sheet.onAbandoned()
    sheet.dismiss()

    assertEquals(listOf(listOf(Cancelled)), reporter.finishes)
  }

  @Test
  fun abandoningAPendingOpeningCancelsIt() {
    sheet.open()
    sheet.onAbandoned()

    assertEquals(listOf(listOf(Cancelled)), reporter.finishes)
  }

  @Test
  fun stoppedOpeningCancelsWithoutHidingTheSheet() {
    sheet.open()
    sheet.cancelOpening()
    sheet.onEnterFinished()

    assertTrue(sheet.visible)
    assertEquals(listOf(listOf(Cancelled)), reporter.finishes)

    sheet.dismiss()
    sheet.open()
    sheet.onEnterFinished()

    assertEquals(listOf(listOf(Cancelled), listOf(Ok)), reporter.finishes)
  }

  @Test
  fun dismissWithoutAnOpeningRecordsNothing() {
    sheet.dismiss()
    sheet.onEnterFinished()
    sheet.onForgotten()

    assertFalse(sheet.visible)
    assertTrue(reporter.started.isEmpty())
  }

  private class RecordingReporter : TelemetryReporter by NoOpTelemetryReporter {
    val started = mutableListOf<Pair<String, String>>()
    private val spans = mutableListOf<RecordingSpan>()
    val finishes: List<List<TelemetrySpanStatus>> get() = spans.map { it.statuses }

    override fun startSpan(operation: String, description: String): TelemetrySpan {
      started += operation to description
      return RecordingSpan().also { spans += it }
    }
  }

  private class RecordingSpan : TelemetrySpan {
    val statuses = mutableListOf<TelemetrySpanStatus>()

    override fun finish(status: TelemetrySpanStatus) {
      statuses += status
    }
  }
}
