/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.overview

import dev.staticvar.vlr.sharedui.component.common.parseMatchPreviewTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.TimeZone

class NewsPreviewDateFormatterTest {
  @Test
  fun parsesIsoDateInProvidedTimeZone() {
    val date = parseMatchPreviewTime("2026-06-13T18:30:00Z", TimeZone.of("Asia/Kolkata"))
    assertEquals("2026-06-14", date?.date.toString())
  }

  @Test
  fun leavesRelativeAndMissingDatesForDisplayFallback() {
    assertNull(parseMatchPreviewTime("2h ago"))
    assertNull(parseMatchPreviewTime(""))
  }
}
