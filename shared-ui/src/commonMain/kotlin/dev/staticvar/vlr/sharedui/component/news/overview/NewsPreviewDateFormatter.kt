/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.overview

import dev.staticvar.vlr.sharedui.component.common.parseMatchPreviewTime
import dev.staticvar.vlr.sharedui.component.common.previewMonthNames
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_news_preview_date
import vlr.shared_ui.generated.resources.format_recent_lower

@androidx.compose.runtime.Composable
internal fun formatNewsPreviewDate(rawDate: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
  val trimmedDate = rawDate.trim()
  if (trimmedDate.isBlank()) return stringResource(Res.string.format_recent_lower)
  val dateTime = parseMatchPreviewTime(trimmedDate, timeZone) ?: return trimmedDate
  return stringResource(
    Res.string.format_news_preview_date,
    previewMonthNames()[dateTime.month.ordinal],
    dateTime.dayOfMonth,
    dateTime.year,
  )
}
