/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.calendar

import androidx.compose.runtime.Composable
import dev.staticvar.vlr.domain.model.MatchDetails
import dev.staticvar.vlr.sharedui.calendar.matchCalendarEntry
import dev.staticvar.vlr.sharedui.calendar.toICalendar
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

public data class MatchCalendarEvent(
  val matchId: String,
  val title: String,
  val description: String,
  val start: Instant,
)

internal val MatchCalendarEvent.end: Instant
  get() = start + 1.hours

internal fun MatchDetails.calendarStart(): Instant? {
  val start = event.date?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
  return start.takeIf { id.isNotBlank() && it.toLocalDateTime(TimeZone.UTC).year in 1..9999 }
}

public fun MatchDetails.toCalendarEvent(versus: String): MatchCalendarEvent? {
  val start = calendarStart() ?: return null
  return MatchCalendarEvent(
    matchId = id,
    title = teams.joinToString(" $versus ") { it.name }.ifBlank { event.name },
    description = listOf(event.name, event.series, event.stage, note).filter(String::isNotBlank).joinToString("\n"),
    start = start,
  )
}

/** Opens the calendar editor or shares ICS on failure. Success means handoff, not a saved event. */
@Composable
public expect fun rememberMatchCalendarExporter(): (MatchCalendarEvent) -> Result<Unit>

internal fun openCalendarWithIcsFallback(
  openCalendar: () -> Unit,
  shareIcs: () -> Unit,
): Result<Unit> = runCatching(openCalendar).recoverCatching { shareIcs() }

internal val MatchCalendarEvent.fileName: String
  get() = "match-${matchId.encodeToByteArray().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }}.ics"

internal fun MatchCalendarEvent.toICalendar(generatedAt: Instant = Clock.System.now()): String =
  listOf(matchCalendarEntry(matchId, title, description, start)).toICalendar(generatedAt)
