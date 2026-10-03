/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.staticvar.vlr.sharedui.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.coroutines.resume
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpan
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970

@Composable
public actual fun rememberCalendarExporter(): suspend (fileName: String, entries: List<CalendarEntry>) -> CalendarExportResult =
  remember {
    { _, entries ->
      try {
        withContext(Dispatchers.Main) { calendarExportMutex.withLock { addToCalendar(entries) } }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        CalendarExportResult.Failed
      }
    }
  }

private val calendarExportMutex = Mutex()

private suspend fun addToCalendar(entries: List<CalendarEntry>): CalendarExportResult {
  val store = EKEventStore()
  val granted = suspendCancellableCoroutine { continuation ->
    if (NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 17 }) {
      store.requestFullAccessToEventsWithCompletion { granted, _ -> continuation.resume(granted) }
    } else {
      store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ -> continuation.resume(granted) }
    }
  }
  if (!granted) return CalendarExportResult.Denied
  val defaults = NSUserDefaults.standardUserDefaults
  val saved = defaults.dictionaryForKey(SAVED_EVENT_IDS_KEY).orEmpty().entries
    .mapNotNull { (uid, record) ->
      if (uid !is String || record !is Map<*, *>) return@mapNotNull null
      val id = (record["id"] as? String)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
      val start = (record["start"] as? String)?.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return@mapNotNull null
      uid to SavedCalendarEvent(id, start)
    }
    .toMap().toMutableMap()
  var changed = false
  val events = entries.distinctBy(CalendarEntry::uid).map { entry ->
    val record = saved[entry.uid]
    val existing = record?.let { store.eventWithIdentifier(it.id) }
      ?: record?.let { store.findMatch(entry.url, it.startEpochSeconds) }
    val start = NSDate.dateWithTimeIntervalSince1970(entry.start.toEpochMilliseconds() / 1000.0)
    val end = NSDate.dateWithTimeIntervalSince1970(entry.end.toEpochMilliseconds() / 1000.0)
    val url = entry.url?.let { NSURL.URLWithString(it) }
    if (existing != null && existing.startDate == start && existing.endDate == end && existing.URL == url &&
      existing.title == entry.title && existing.notes == entry.description
    ) {
      return@map entry to existing
    }
    val event = existing ?: EKEvent.eventWithEventStore(store).apply {
      calendar = checkNotNull(store.defaultCalendarForNewEvents) { "No calendar accepts new events." }
    }
    event.title = entry.title
    event.notes = entry.description
    event.startDate = start
    event.endDate = end
    event.URL = url
    check(store.saveEvent(event, span = EKSpan.EKSpanThisEvent, commit = false, error = null)) {
      "Unable to prepare the calendar event."
    }
    changed = true
    entry to event
  }
  if (changed) check(store.commit(null)) { "Unable to save the calendar events." }
  events.forEach { (entry, event) ->
    saved[entry.uid] = SavedCalendarEvent(checkNotNull(event.eventIdentifier), entry.start.epochSeconds.toDouble())
  }
  defaults.setObject(
    saved.mapValues { (_, event) -> mapOf("id" to event.id, "start" to event.startEpochSeconds.toString()) },
    forKey = SAVED_EVENT_IDS_KEY,
  )
  return if (changed) CalendarExportResult.Added else CalendarExportResult.AlreadyAdded
}

private fun EKEventStore.findMatch(url: String?, lastStartEpochSeconds: Double): EKEvent? {
  if (url == null) return null
  val predicate = predicateForEventsWithStartDate(
    NSDate.dateWithTimeIntervalSince1970(lastStartEpochSeconds - 86_400),
    endDate = NSDate.dateWithTimeIntervalSince1970(lastStartEpochSeconds + 86_400),
    calendars = null,
  )
  return eventsMatchingPredicate(predicate).filterIsInstance<EKEvent>().firstOrNull { it.URL?.absoluteString == url }
}

private data class SavedCalendarEvent(val id: String, val startEpochSeconds: Double)

private const val SAVED_EVENT_IDS_KEY = "calendar.savedEventIds"
