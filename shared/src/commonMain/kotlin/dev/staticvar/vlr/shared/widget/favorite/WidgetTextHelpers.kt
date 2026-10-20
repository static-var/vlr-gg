/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.LocalDate

internal fun meaningfulText(value: String?): String? = value?.trim()?.takeUnless {
  it.isEmpty() || it == "-" || it.equals("TBD", ignoreCase = true)
}

internal fun nameKey(value: String): String = value.trim().lowercase()

internal fun dotJoin(vararg values: String?): String =
  values.filterNotNull().filter(String::isNotEmpty).joinToString(" · ")

internal fun logoMark(value: WidgetTeam): LogoMark.Named =
  LogoMark.Named(value.name, value.tag, value.logo, (value.tag ?: value.name.firstCodePoints(3).trimEnd()).uppercase())

internal fun pillTone(value: WidgetMapStatus): PillTone = when (value) {
  WidgetMapStatus.Won -> PillTone.Won
  WidgetMapStatus.Lost -> PillTone.Lost
  WidgetMapStatus.Live -> PillTone.Live
  WidgetMapStatus.Next -> PillTone.Next
}

internal fun dayText(value: LocalDate?, locale: WidgetLocale): String =
  value?.let { locale.date(it, DateSkeleton.MonthDay) } ?: locale.text(WidgetTextKey.Tbd)

internal fun signedInteger(value: Int, locale: WidgetLocale): String = when {
  value > 0 -> "+" + locale.integer(value)
  value < 0 -> "−" + locale.integer(-value.toLong())
  else -> locale.integer(0)
}

private fun String.firstCodePoints(count: Int): String {
  var end = 0
  repeat(count) {
    if (end == length) return substring(0, end)
    val first = this[end++]
    if (first.isHighSurrogate() && end < length && this[end].isLowSurrogate()) end++
  }
  return substring(0, end)
}
