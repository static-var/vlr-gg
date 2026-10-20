/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.shared.widget.favorite

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal data class WidgetDay private constructor(val date: LocalDate) : Comparable<WidgetDay> {
  val year: Int get() = date.year
  val month: Int get() = date.month.number
  val day: Int get() = date.day

  fun start(zone: TimeZone): Instant = date.atStartOfDayIn(zone)
  override fun compareTo(other: WidgetDay): Int = date.compareTo(other.date)

  companion object {
    fun parse(value: String): WidgetDay? {
      if (!Regex("\\d{4}-\\d{1,2}-\\d{1,2}").matches(value)) return null
      val parts = value.split('-').map { it.toInt() }
      return of(parts[0], parts[1], parts[2])
    }

    fun of(year: Int, month: Int, day: Int): WidgetDay? {
      if (year !in 1..9999) return null
      return try {
        WidgetDay(LocalDate(year, month, day))
      } catch (_: IllegalArgumentException) {
        null
      }
    }
  }
}

internal data class EventDates(val start: WidgetDay?, val endInclusive: WidgetDay?)

internal sealed interface Kickoff {
  data class Scheduled(val day: String, val time: String) : Kickoff
  data class Delayed(val originalTime: String) : Kickoff
  data object Unknown : Kickoff

  fun hero(locale: WidgetLocale): String = when (this) {
    is Scheduled -> time
    is Delayed -> locale.text(WidgetTextKey.AnyMinute)
    Unknown -> locale.text(WidgetTextKey.Tbd)
  }
}

internal object WidgetTemporalRules {
  private val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

  fun parseInstant(value: String?): Instant? {
    if (value == null || !value.contains('T')) return null
    return try {
      Instant.parse(value)
    } catch (_: IllegalArgumentException) {
      null
    }
  }

  fun matchStart(date: String, time: String): Instant? {
    val clock = time.trim()
    if ('T' in clock) {
      if (WidgetDay.parse(clock.take(10)) == null) return null
      return parseInstant(clock)
    }
    val day = WidgetDay.parse(date) ?: return null
    if (!Regex("\\d{2}:\\d{2}(:\\d{2})?").matches(clock)) return null
    val parts = clock.split(':').map { it.toInt() }
    if (parts[0] >= 24 || parts[1] >= 60 || parts.getOrElse(2) { 0 } >= 60) return null
    return LocalDateTime(
      day.year,
      day.month,
      day.day,
      parts[0],
      parts[1],
      parts.getOrElse(2) {
        0
      },
    ).toInstant(TimeZone.UTC)
  }

  fun eventDates(text: String, matchDays: List<LocalDate>, reference: Instant): EventDates {
    val days = matchDays.mapNotNull { WidgetDay.of(it.year, it.month.number, it.day) }
    val fallback = EventDates(days.minOrNull(), days.maxOrNull())
    val parts = text.replace('–', '-').replace('—', '-').split('-').map(String::trim)
    if (parts.size != 2) return fallback
    val left = bound(parts[0], null) ?: return fallback
    val right = bound(parts[1], left.month)
    if (right == null && !parts[1].equals("TBD", ignoreCase = true)) return fallback
    val anchor = days.minOrNull()?.date ?: reference.toLocalDateTime(TimeZone.UTC).date
    val anchorInEndYear = right != null && right.month < left.month && anchor.month.number <= right.month
    val startYear = left.year ?: right?.year?.let { it - if (left.month > right.month) 1 else 0 }
      ?: (anchor.year - if (anchorInEndYear) 1 else 0)
    val start = WidgetDay.of(startYear, left.month, left.day) ?: return fallback
    val end = if (right != null) {
      val year = right.year ?: (startYear + if (right.month < left.month) 1 else 0)
      val parsed = WidgetDay.of(year, right.month, right.day) ?: return fallback
      if (parsed < start) return fallback
      parsed
    } else {
      days.maxOrNull()
    }
    return EventDates(start, end?.takeIf { it >= start })
  }

  private data class Bound(val month: Int, val day: Int, val year: Int?)

  private fun bound(value: String, month: Int?): Bound? {
    val tokens = value.replace(',', ' ').split(Regex("\\s+")).filter(String::isNotEmpty)
    if (tokens.isEmpty() || tokens.size > 3) return null
    val parsedMonth = months.indexOf(tokens[0].take(3).lowercase()).takeIf { it >= 0 }?.plus(1)
    val resolvedMonth = parsedMonth ?: month ?: return null
    val dayIndex = if (parsedMonth == null) 0 else 1
    val day = tokens.getOrNull(dayIndex)?.toIntOrNull() ?: return null
    if (tokens.size > dayIndex + 2) return null
    val year = if (tokens.size == dayIndex + 2) {
      val token = tokens[dayIndex + 1]
      if (token.length != 4) return null
      token.toIntOrNull()?.takeIf { it > 0 } ?: return null
    } else {
      null
    }
    return Bound(resolvedMonth, day, year)
  }

  fun nextMidnight(now: Instant, zone: TimeZone): Instant = now.toLocalDateTime(zone).date
    .plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone)

  fun kickoff(start: Instant?, now: Instant, zone: TimeZone, locale: WidgetLocale): Kickoff {
    if (start == null) return Kickoff.Unknown
    val time = locale.instant(start, TimeSkeleton.HourMinute, zone)
    if (start <= now) return Kickoff.Delayed(time)
    val days = now.toLocalDateTime(zone).date.daysUntil(start.toLocalDateTime(zone).date)
    val day = if (days ==
      0
    ) {
      locale.relativeDay(RelativeDay.Today)
    } else if (days ==
      1
    ) {
      locale.relativeDay(RelativeDay.Tomorrow)
    } else {
      locale.date(start.toLocalDateTime(zone).date, DateSkeleton.WeekdayMonthDay)
    }
    return Kickoff.Scheduled(day, time)
  }

  fun rowClock(start: Instant, now: Instant, zone: TimeZone, locale: WidgetLocale): Pair<String, String> {
    val local = start.toLocalDateTime(zone)
    val days = now.toLocalDateTime(zone).date.daysUntil(local.date)
    val day = when {
      days == -1 -> locale.relativeDay(RelativeDay.Yesterday)
      days == 0 -> locale.relativeDay(RelativeDay.Today)
      days == 1 -> locale.relativeDay(RelativeDay.Tomorrow)
      days in -6..6 -> locale.date(local.date, DateSkeleton.Weekday)
      else -> locale.date(local.date, DateSkeleton.MonthDay)
    }
    return day to locale.instant(start, if (local.minute == 0) TimeSkeleton.Hour else TimeSkeleton.HourMinute, zone)
  }

  fun kickoffDates(start: Instant?, now: Instant, zone: TimeZone): List<Instant> {
    if (start == null || start <= now) return listOf(now)
    val day = start.toLocalDateTime(zone).date
    val changes = listOf(day.minus(1, DateTimeUnit.DAY).atStartOfDayIn(zone), day.atStartOfDayIn(zone), start)
    return listOf(now) + changes.filter { it > now }.distinct().sorted()
  }

  fun scheduled(start: Instant?, now: Instant, overdueInterval: Duration): Instant {
    if (start == null) return now + 7200.seconds
    val remaining = start - now
    val interval = when {
      remaining <= Duration.ZERO -> overdueInterval
      remaining <= 86400.seconds -> 1800.seconds
      else -> 7200.seconds
    }
    return if (start > now) minOf(now + interval, start) else now + interval
  }
}
