/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.calendar

import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.sharedui.calendar.CalendarEntry
import dev.staticvar.vlr.sharedui.calendar.matchCalendarEntry
import kotlin.time.Clock
import kotlin.time.Instant

// The file name is the title users see in the system preview and share sheet.
internal val EventDetails.calendarFileName: String
  get() = title.map { if (it.isLetterOrDigit()) it else ' ' }.joinToString("").trim()
    .split(Regex(" +")).joinToString("-").take(60).ifEmpty { "event-matches" } + ".ics"

/** Matches that have a known start time in the future. Finished and unscheduled matches are left out. */
internal fun EventDetails.upcomingCalendarEntries(
  versus: String,
  now: Instant = Clock.System.now(),
): List<CalendarEntry> = matches
  .filter { it.matchId.isNotBlank() && it.status.trim().lowercase() !in FINISHED_STATUSES }
  .distinctBy(EventMatch::matchId)
  .mapNotNull { match ->
    val start = match.calendarStart()?.takeIf { it > now } ?: return@mapNotNull null
    val teams = match.teams.map { it.name.trim() }.filter(String::isNotEmpty)
    val context = listOf(match.stage, match.round).map(String::trim).filter(String::isNotEmpty)
    matchCalendarEntry(
      matchId = match.matchId,
      title = if (teams.any { !it.equals(UNDECIDED_TEAM, ignoreCase = true) }) {
        teams.joinToString(" $versus ")
      } else {
        (context.takeLast(1) + title).joinToString(" · ")
      },
      description = (listOf(title) + context).filter(String::isNotBlank).joinToString("\n"),
      start = start,
    )
  }
  .sortedBy(CalendarEntry::start)

// The API sends a UTC date and time, or a full timestamp in the time field.
internal fun EventMatch.calendarStart(): Instant? {
  val time = time.trim()
  val timestamp = when {
    'T' in time -> time
    TIME_OF_DAY.matches(time) -> "${date.trim()}T${if (time.length == 5) "$time:00" else time}Z"
    else -> return null
  }
  return runCatching { Instant.parse(timestamp) }.getOrNull()
    ?.takeIf { it.toString().substringBefore('-').length == 4 }
}

private val FINISHED_STATUSES = setOf("completed", "final")
private val TIME_OF_DAY = Regex("""\d{2}:\d{2}(:\d{2})?""")
private const val UNDECIDED_TEAM = "TBD"
