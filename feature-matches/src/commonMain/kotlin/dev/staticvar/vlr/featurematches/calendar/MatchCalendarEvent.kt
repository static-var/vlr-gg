/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.calendar

import androidx.compose.runtime.Composable
import dev.staticvar.vlr.domain.model.MatchDetails
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

public data class MatchCalendarEvent(
  val matchId: String,
  val title: String,
  val description: String,
  val start: Instant,
)

public fun MatchDetails.toCalendarEvent(): MatchCalendarEvent? {
  val start = event.date?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
  if (id.isBlank() || start.toLocalDateTime(TimeZone.UTC).year !in 1..9999) return null
  return MatchCalendarEvent(
    matchId = id,
    title = teams.joinToString(" vs ") { it.name }.ifBlank { event.name },
    description = listOf(event.name, event.series, event.stage, note).filter(String::isNotBlank).joinToString("\n"),
    start = start,
  )
}

/** Opens a system export surface. Success means handoff, not that the user saved an event. */
@Composable
public expect fun rememberMatchCalendarExporter(): (MatchCalendarEvent) -> Result<Unit>

internal val MatchCalendarEvent.fileName: String
  get() = "match-${matchId.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }}.ics"

internal fun MatchCalendarEvent.toICalendar(generatedAt: Instant = Clock.System.now()): String {
  require(matchId.isNotBlank())
  val uid = matchId.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
  return buildList {
    addAll(
      listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//VLR App//Match Calendar//EN",
        "CALSCALE:GREGORIAN",
        "BEGIN:VEVENT",
        "UID:match-$uid@vlr.gg",
        "DTSTAMP:${generatedAt.calendarTimestamp()}",
        "DTSTART:${start.calendarTimestamp()}",
        "SUMMARY:${title.calendarText()}",
        "DESCRIPTION:${description.calendarText()}",
      ),
    )
    if (matchId.all(Char::isDigit)) add("URL:https://www.vlr.gg/$matchId")
    add("END:VEVENT")
    add("END:VCALENDAR")
  }.joinToString(separator = "\r\n", postfix = "\r\n") { it.foldCalendarLine() }
}

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
