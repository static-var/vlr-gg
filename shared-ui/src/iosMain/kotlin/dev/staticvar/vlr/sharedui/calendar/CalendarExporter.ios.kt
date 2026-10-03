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
    .mapNotNull { (uid, id) -> if (uid is String && id is String) uid to id else null }
    .toMap().toMutableMap()
  val changed = entries.distinctBy(CalendarEntry::uid).mapNotNull { entry ->
    val existing = saved[entry.uid]?.let(store::eventWithIdentifier)
    val start = NSDate.dateWithTimeIntervalSince1970(entry.start.toEpochMilliseconds() / 1000.0)
    val end = NSDate.dateWithTimeIntervalSince1970(entry.end.toEpochMilliseconds() / 1000.0)
    val url = entry.url?.let { NSURL.URLWithString(it) }
    if (existing != null && existing.startDate == start && existing.endDate == end &&
      existing.title == entry.title && existing.notes == entry.description && existing.URL == url
    ) return@mapNotNull null
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
    entry.uid to event
  }
  if (changed.isEmpty()) return CalendarExportResult.AlreadyAdded
  check(store.commit(null)) { "Unable to save the calendar events." }
  changed.forEach { (uid, event) -> saved[uid] = checkNotNull(event.eventIdentifier) }
  defaults.setObject(saved, forKey = SAVED_EVENT_IDS_KEY)
  return CalendarExportResult.Added
}

private const val SAVED_EVENT_IDS_KEY = "calendar.savedEventIds"
