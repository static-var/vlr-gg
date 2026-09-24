/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.runtime.Composable
import kotlin.time.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_match_preview_date
import vlr.shared_ui.generated.resources.format_month_apr
import vlr.shared_ui.generated.resources.format_month_aug
import vlr.shared_ui.generated.resources.format_month_dec
import vlr.shared_ui.generated.resources.format_month_feb
import vlr.shared_ui.generated.resources.format_month_jan
import vlr.shared_ui.generated.resources.format_month_jul
import vlr.shared_ui.generated.resources.format_month_jun
import vlr.shared_ui.generated.resources.format_month_mar
import vlr.shared_ui.generated.resources.format_month_may
import vlr.shared_ui.generated.resources.format_month_nov
import vlr.shared_ui.generated.resources.format_month_oct
import vlr.shared_ui.generated.resources.format_month_sep

@Composable
public fun formatMatchPreviewTime(isoUtcTime: String?, timeZone: TimeZone = TimeZone.currentSystemDefault()): String? {
  val dateTime = parseMatchPreviewTime(isoUtcTime, timeZone) ?: return null
  return stringResource(
    Res.string.format_match_preview_date,
    previewMonthName(dateTime.month),
    dateTime.dayOfMonth,
    dateTime.hour.twoDigits(),
    dateTime.minute.twoDigits(),
  )
}

internal fun parseMatchPreviewTime(
  isoUtcTime: String?,
  timeZone: TimeZone = TimeZone.currentSystemDefault(),
): kotlinx.datetime.LocalDateTime? = isoUtcTime?.takeIf(String::isNotBlank)
  ?.let { runCatching { Instant.parse(it).toLocalDateTime(timeZone) }.getOrNull() }

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')

@Composable
internal fun previewMonthName(month: Month): String = stringResource(previewMonthResources[month.ordinal])

private val previewMonthResources = listOf(
  Res.string.format_month_jan,
  Res.string.format_month_feb,
  Res.string.format_month_mar,
  Res.string.format_month_apr,
  Res.string.format_month_may,
  Res.string.format_month_jun,
  Res.string.format_month_jul,
  Res.string.format_month_aug,
  Res.string.format_month_sep,
  Res.string.format_month_oct,
  Res.string.format_month_nov,
  Res.string.format_month_dec,
)

public suspend fun getMatchPreviewTime(
  isoUtcTime: String?,
  timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String? {
  val dateTime = parseMatchPreviewTime(isoUtcTime, timeZone) ?: return null
  return getString(
    Res.string.format_match_preview_date,
    getString(previewMonthResources[dateTime.month.ordinal]),
    dateTime.dayOfMonth,
    dateTime.hour.twoDigits(),
    dateTime.minute.twoDigits(),
  )
}
