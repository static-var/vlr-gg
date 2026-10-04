/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.calendar

import androidx.compose.runtime.Composable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

public data class CalendarEntry(
  val uid: String,
  val title: String,
  val description: String,
  val start: Instant,
  val end: Instant,
  val url: String? = null,
)

/** Calendar entry for a match. The UID depends only on the match ID, so a repeated import updates the entry. */
public fun matchCalendarEntry(matchId: String, title: String, description: String, start: Instant): CalendarEntry {
  return CalendarEntry(
    uid = matchCalendarUid(matchId),
    title = title,
    description = description,
    start = start,
    end = start + 1.hours,
    url = "https://valorantesports.staticvar.dev/match/$matchId".takeIf { matchId.all(Char::isDigit) },
  )
}

public fun matchCalendarUid(matchId: String): String {
  require(matchId.isNotBlank())
  val uid = matchId.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
  return "match-$uid@valorantesports.staticvar.dev"
}

/** Uses the metadata shared by match details and event schedules. */
public fun matchCalendarEntry(
  matchId: String,
  eventName: String,
  teamNames: List<String>,
  versus: String,
  start: Instant,
): CalendarEntry {
  val teams = teamNames.map(String::trim).filter(String::isNotEmpty)
    .map { if (it.equals("TBD", ignoreCase = true)) "TBD" else it }
  val event = eventName.trim()
  return matchCalendarEntry(
    matchId = matchId,
    title = if (teams.any { it != "TBD" }) teams.joinToString(" ${versus.trim()} ") else event,
    description = event,
    start = start,
  )
}

public enum class CalendarExportResult {
  /** The entries were handed to another app; whether they were saved is unknown. */
  Opened,
  Added,
  AlreadyAdded,
  Removed,
  Denied,
  Failed,
}

public enum class CalendarEntryStatus {
  NotAdded,
  /** At least one requested match saved by this app is still in the calendar. */
  Added,
  Denied,
}

public interface CalendarManager {
  public suspend fun status(uids: Set<String>): CalendarEntryStatus

  public suspend fun add(fileName: String, entries: List<CalendarEntry>): CalendarExportResult

  public suspend fun remove(uids: Set<String>): CalendarExportResult
}

@Composable
public expect fun rememberCalendarManager(): CalendarManager

/**
 * Adds entries to the user's calendar. Android hands an ICS file to a calendar app, with share targets as the
 * fallback. iOS saves or reschedules entries with calendar access, checking saved events to avoid duplicates.
 */
@Composable
public expect fun rememberCalendarExporter(): suspend (fileName: String, entries: List<CalendarEntry>) -> CalendarExportResult

public fun List<CalendarEntry>.toICalendar(generatedAt: Instant = Clock.System.now()): String = buildList {
  add("BEGIN:VCALENDAR")
  add("VERSION:2.0")
  add("PRODID:-//Val Esports//Match Calendar//EN")
  add("CALSCALE:GREGORIAN")
  this@toICalendar.forEach { entry ->
    add("BEGIN:VEVENT")
    add("UID:${entry.uid}")
    add("DTSTAMP:${generatedAt.calendarTimestamp()}")
    add("DTSTART:${entry.start.calendarTimestamp()}")
    add("DTEND:${entry.end.calendarTimestamp()}")
    add("SUMMARY:${entry.title.calendarText()}")
    add("DESCRIPTION:${entry.description.calendarText()}")
    entry.url?.let { add("URL:$it") }
    add("END:VEVENT")
  }
  add("END:VCALENDAR")
}.joinToString(separator = "\r\n", postfix = "\r\n") { it.foldCalendarLine() }

private fun Instant.calendarTimestamp(): String {
  val value = toLocalDateTime(TimeZone.UTC)
  require(value.year in 1..9999)
  fun Int.padded(length: Int = 2): String = toString().padStart(length, '0')
  return "${value.year.padded(4)}${value.monthNumber.padded()}${value.dayOfMonth.padded()}T" +
    "${value.hour.padded()}${value.minute.padded()}${value.second.padded()}Z"
}

private fun String.calendarText(): String = replace("\r\n", "\n").replace('\r', '\n')
  .replace("\\", "\\\\").replace("\n", "\\n").replace(";", "\\;").replace(",", "\\,")

// RFC 5545 counts UTF-8 octets; keep surrogate pairs together when folding Unicode text.
private fun String.foldCalendarLine(): String = buildString {
  var octets = 0
  var index = 0
  while (index < this@foldCalendarLine.length) {
    val length = if (this@foldCalendarLine[index].isHighSurrogate() &&
      index + 1 < this@foldCalendarLine.length && this@foldCalendarLine[index + 1].isLowSurrogate()
    ) 2 else 1
    val character = this@foldCalendarLine.substring(index, index + length)
    val size = character.encodeToByteArray().size
    if (octets + size > 75) {
      append("\r\n ")
      octets = 1
    }
    append(character)
    octets += size
    index += length
  }
}
