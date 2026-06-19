/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.overview

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class NewsPreviewDateFormatterTest {
  @Test
  fun formatsIsoDateInProvidedTimeZone() {
    val formattedDate = formatNewsPreviewDate(
      rawDate = "2026-06-13T18:30:00Z",
      timeZone = TimeZone.of("Asia/Kolkata"),
    )

    assertEquals("Jun 14, 2026", formattedDate)
  }

  @Test
  fun preservesAlreadyReadableOrInvalidDates() {
    assertEquals("2h ago", formatNewsPreviewDate(rawDate = "2h ago"))
    assertEquals("recent", formatNewsPreviewDate(rawDate = ""))
  }
}
