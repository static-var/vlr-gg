/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.domain.model.EventMatch
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.featureevents.calendar.calendarStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Updates the local day at midnight, including days changed by daylight saving time. */
internal fun eventLocalDateFlow(
  timeZone: TimeZone,
  now: () -> Instant = Clock.System::now,
): Flow<LocalDate> = flow {
  while (true) {
    val instant = now()
    val date = instant.toLocalDateTime(timeZone).date
    emit(date)
    delay(date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone) - instant)
  }
}

internal enum class EventMatchOrder {
  OldestFirst,
  NewestFirst,
  ;

  val toggled: EventMatchOrder
    get() = if (this == OldestFirst) NewestFirst else OldestFirst
}

/** Control shown at the end of the Matches header. */
internal enum class EventMatchesAction {
  None,
  Today,
  Order,
}

internal val EventStatus.matchesAction: EventMatchesAction
  get() = when (this) {
    EventStatus.ONGOING -> EventMatchesAction.Today
    EventStatus.COMPLETED -> EventMatchesAction.Order
    else -> EventMatchesAction.None
  }

/** Only completed events let users flip the order; everything else reads oldest first. */
internal fun EventStatus.effectiveMatchOrder(selected: EventMatchOrder): EventMatchOrder =
  if (matchesAction == EventMatchesAction.Order) selected else EventMatchOrder.OldestFirst

internal data class ScheduledEventMatch(val match: EventMatch, val start: Instant?)

/** Matches that share a local calendar day. A null [date] collects matches without a usable date. */
internal data class EventMatchDay(val date: LocalDate?, val matches: List<ScheduledEventMatch>)

/**
 * Every match in one chronological list, split into local calendar days.
 *
 * Timed matches use the same UTC parser as calendar export and land on the viewer's local day. A
 * match with a date but no time keeps the published date and follows that day's timed matches.
 * Matches without a usable date come last in their original order. Ties keep the server order.
 */
internal fun List<EventMatch>.chronologicalMatchDays(
  timeZone: TimeZone,
  order: EventMatchOrder = EventMatchOrder.OldestFirst,
): List<EventMatchDay> {
  val scheduled = map { match ->
    val start = match.calendarStart()
    DatedMatch(ScheduledEventMatch(match, start), start?.toLocalDateTime(timeZone)?.date ?: match.publishedDate())
  }
  val (dated, undated) = scheduled.partition { it.date != null }
  val chronological = dated.sortedWith(
    compareBy<DatedMatch>({ it.date }, { it.scheduled.start == null }, { it.scheduled.start }),
  )
  val ordered = if (order == EventMatchOrder.NewestFirst) chronological.asReversed() else chronological
  val days = ordered.groupBy({ it.date }, { it.scheduled }).map { (date, matches) -> EventMatchDay(date, matches) }
  return if (undated.isEmpty()) days else days + EventMatchDay(date = null, matches = undated.map { it.scheduled })
}

private data class DatedMatch(val scheduled: ScheduledEventMatch, val date: LocalDate?)

private fun EventMatch.publishedDate(): LocalDate? = runCatching { LocalDate.parse(date.trim()) }.getOrNull()
