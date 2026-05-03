/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchTimeFormatterTest {
  @Test
  fun formatsUtcMatchTimeInProvidedTimeZone() {
    val formattedTime = formatMatchPreviewTime(
      isoUtcTime = "2025-10-03T11:00:00Z",
      timeZone = TimeZone.of("Asia/Kolkata"),
    )

    assertEquals("Oct 3, 16:30", formattedTime)
  }

  @Test
  fun returnsNullForMissingOrInvalidTime() {
    assertNull(formatMatchPreviewTime(isoUtcTime = null))
    assertNull(formatMatchPreviewTime(isoUtcTime = ""))
    assertNull(formatMatchPreviewTime(isoUtcTime = "14:00 CET"))
  }
}
