/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.staticvar.vlr.sharedui.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusNotDetermined
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpan
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.coroutines.resume

@Composable
public actual fun rememberCalendarManager(): CalendarManager = remember { IosCalendarManager }

@Composable
public actual fun rememberCalendarExporter(): suspend (
  fileName: String,
  entries: List<CalendarEntry>,
) -> CalendarExportResult {
  val manager = rememberCalendarManager()
  return remember(manager) { { fileName, entries -> manager.add(fileName, entries) } }
}

private object IosCalendarManager : CalendarManager {
  override suspend fun status(uids: Set<String>): CalendarEntryStatus = withContext(Dispatchers.Main) {
    calendarExportMutex.withLock {
      when (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)) {
        EKAuthorizationStatusNotDetermined -> CalendarEntryStatus.NotAdded

        EKAuthorizationStatusAuthorized -> {
          val store = EKEventStore()
          val saved = readSavedEvents()
          if (uids.any { uid -> saved[uid]?.let { store.findOwnedEvent(it) } != null }) {
            CalendarEntryStatus.Added
          } else {
            CalendarEntryStatus.NotAdded
          }
        }

        else -> CalendarEntryStatus.Denied
      }
    }
  }

  override suspend fun add(fileName: String, entries: List<CalendarEntry>): CalendarExportResult =
    mutateCalendar { store ->
      store.addToCalendar(entries)
    }

  override suspend fun remove(uids: Set<String>): CalendarExportResult = mutateCalendar { store ->
    val saved = readSavedEvents()
    val removed = mutableSetOf<String>()
    uids.forEach { uid ->
      saved[uid]?.let { store.findOwnedEvent(it) }?.let { event ->
        check(store.removeEvent(event, span = EKSpan.EKSpanThisEvent, commit = false, error = null)) {
          "Unable to prepare calendar removal."
        }
        removed += uid
      }
    }
    if (removed.isNotEmpty()) check(store.commit(null)) { "Unable to remove calendar events." }
    removed.forEach(saved::remove)
    writeSavedEvents(saved)
    CalendarExportResult.Removed
  }
}

private val calendarExportMutex = Mutex()

private suspend fun mutateCalendar(action: (EKEventStore) -> CalendarExportResult): CalendarExportResult = try {
  withContext(Dispatchers.Main) {
    calendarExportMutex.withLock {
      val store = EKEventStore()
      if (store.requestCalendarAccess()) action(store) else CalendarExportResult.Denied
    }
  }
} catch (cancelled: CancellationException) {
  throw cancelled
} catch (_: Exception) {
  CalendarExportResult.Failed
}

private suspend fun EKEventStore.requestCalendarAccess(): Boolean {
  if (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent) ==
    EKAuthorizationStatusAuthorized
  ) {
    return true
  }
  return suspendCancellableCoroutine { continuation ->
    if (NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 17 }) {
      requestFullAccessToEventsWithCompletion { granted, _ -> continuation.resume(granted) }
    } else {
      requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ -> continuation.resume(granted) }
    }
  }
}

private fun EKEventStore.addToCalendar(entries: List<CalendarEntry>): CalendarExportResult {
  val store = this
  val saved = readSavedEvents()
  var changed = false
  val events = entries.distinctBy(CalendarEntry::uid).map { entry ->
    val record = saved[entry.uid]
    val existing = record?.let { store.findOwnedEvent(it) }
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
    saved[entry.uid] = SavedCalendarEvent(
      id = checkNotNull(event.eventIdentifier),
      externalId = event.calendarItemExternalIdentifier,
      calendarId = event.calendar?.calendarIdentifier,
    )
  }
  writeSavedEvents(saved)
  return if (changed) CalendarExportResult.Added else CalendarExportResult.AlreadyAdded
}

private fun EKEventStore.findOwnedEvent(record: SavedCalendarEvent): EKEvent? {
  eventWithIdentifier(record.id)?.let { return it }
  val externalId = record.externalId ?: return null
  return calendarItemsWithExternalIdentifier(externalId).filterIsInstance<EKEvent>()
    .filter { record.matchesFallback(it.calendarItemExternalIdentifier, it.calendar?.calendarIdentifier) }
    .singleOrNull()
}

private fun readSavedEvents(): MutableMap<String, SavedCalendarEvent> =
  NSUserDefaults.standardUserDefaults.dictionaryForKey(SAVED_EVENT_IDS_KEY).orEmpty().entries
    .mapNotNull { (uid, record) ->
      if (uid !is String) return@mapNotNull null
      SavedCalendarEvent.fromRecord(record)?.let { uid to it }
    }
    .toMap().toMutableMap()

private fun writeSavedEvents(saved: Map<String, SavedCalendarEvent>) {
  NSUserDefaults.standardUserDefaults.setObject(
    saved.mapValues { (_, event) -> event.toRecord() },
    forKey = SAVED_EVENT_IDS_KEY,
  )
}

private const val SAVED_EVENT_IDS_KEY = "calendar.savedEventIds"
