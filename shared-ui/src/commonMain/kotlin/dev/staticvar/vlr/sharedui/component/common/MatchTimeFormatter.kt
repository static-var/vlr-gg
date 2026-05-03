/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

public fun formatMatchPreviewTime(isoUtcTime: String?, timeZone: TimeZone = TimeZone.currentSystemDefault()): String? {
  val instant = isoUtcTime
    ?.takeIf(String::isNotBlank)
    ?.let { time -> runCatching { Instant.parse(time) }.getOrNull() }
    ?: return null
  val dateTime = instant.toLocalDateTime(timeZone)

  return "${dateTime.month.name.shortName()} ${dateTime.dayOfMonth}, " +
    "${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}"
}

private fun String.shortName(): String = lowercase()
  .replaceFirstChar { char -> char.uppercase() }
  .take(3)

private fun Int.twoDigits(): String = toString().padStart(length = 2, padChar = '0')
