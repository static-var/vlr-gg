/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.overview

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal fun formatNewsPreviewDate(rawDate: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
  val trimmedDate = rawDate.trim()
  if (trimmedDate.isBlank()) {
    return "recent"
  }

  val instant = runCatching { Instant.parse(trimmedDate) }.getOrNull() ?: return trimmedDate
  val dateTime = instant.toLocalDateTime(timeZone)

  return "${dateTime.month.name.shortName()} ${dateTime.dayOfMonth}, ${dateTime.year}"
}

private fun String.shortName(): String = lowercase()
  .replaceFirstChar { char -> char.uppercase() }
  .take(3)
