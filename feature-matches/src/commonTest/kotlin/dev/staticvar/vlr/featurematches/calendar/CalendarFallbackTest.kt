/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CalendarFallbackTest {
  @Test
  fun successfulEditorHandoffDoesNotShareIcs() {
    var shares = 0
    val result = openCalendarWithIcsFallback(openCalendar = {}, shareIcs = { shares++ })
    assertTrue(result.isSuccess)
    assertEquals(0, shares)
  }

  @Test
  fun editorFailureSharesIcsOnce() {
    var shares = 0
    val result = openCalendarWithIcsFallback(
      openCalendar = { error("No calendar editor") },
      shareIcs = { shares++ },
    )
    assertTrue(result.isSuccess)
    assertEquals(1, shares)
  }

  @Test
  fun failedIcsFallbackReturnsFailureToTheScreen() {
    val failure = IllegalStateException("Unable to share file")
    val result = openCalendarWithIcsFallback(
      openCalendar = { error("No calendar editor") },
      shareIcs = { throw failure },
    )
    assertSame(failure, result.exceptionOrNull())
  }
}
