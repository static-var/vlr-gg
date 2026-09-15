/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.TimeZone

class MatchTimeFormatterTest {
  @Test
  fun formatsUtcMatchTimeInProvidedTimeZone() {
    val formattedTime = parseMatchPreviewTime(
      isoUtcTime = "2025-10-03T11:00:00Z",
      timeZone = TimeZone.of("Asia/Kolkata"),
    )

    assertEquals("2025-10-03T16:30", formattedTime?.toString())
  }

  @Test
  fun returnsNullForMissingOrInvalidTime() {
    assertNull(parseMatchPreviewTime(isoUtcTime = null))
    assertNull(parseMatchPreviewTime(isoUtcTime = ""))
    assertNull(parseMatchPreviewTime(isoUtcTime = "14:00 CET"))
  }
}
