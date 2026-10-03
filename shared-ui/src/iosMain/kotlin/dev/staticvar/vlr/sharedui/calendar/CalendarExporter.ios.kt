/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(
  kotlinx.cinterop.ExperimentalForeignApi::class,
  kotlinx.cinterop.BetaInteropApi::class,
)

package dev.staticvar.vlr.sharedui.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlin.coroutines.resume
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpan
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Composable
public actual fun rememberCalendarExporter(): suspend (fileName: String, entries: List<CalendarEntry>) -> CalendarExportResult {
  val controller = LocalUIViewController.current
  val preview = remember { CalendarFilePreview() }
  return remember(controller, preview) {
    { fileName, entries ->
      runCatching {
        if (NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 17 }) {
          addToCalendar(entries)
        } else {
          // Add-only access needs iOS 17. Older versions can only preview and share the file.
          withContext(Dispatchers.Main) { preview.present(fileName, entries.toICalendar(), controller) }
          CalendarExportResult.Opened
        }
      }.getOrDefault(CalendarExportResult.Failed)
    }
  }
}

private suspend fun addToCalendar(entries: List<CalendarEntry>): CalendarExportResult {
  val store = EKEventStore()
  val granted = suspendCancellableCoroutine { continuation ->
    store.requestWriteOnlyAccessToEventsWithCompletion { granted, _ -> continuation.resume(granted) }
  }
  if (!granted) return CalendarExportResult.Denied
  // Add-only access cannot read the calendar back, so the saved entries are remembered here to avoid duplicates.
  val defaults = NSUserDefaults.standardUserDefaults
  val saved = defaults.stringArrayForKey(SAVED_ENTRIES_KEY).orEmpty().filterIsInstance<String>().toSet()
  val pending = entries.filter { it.savedKey !in saved }
  if (pending.isEmpty()) return CalendarExportResult.AlreadyAdded
  val calendar = checkNotNull(store.defaultCalendarForNewEvents) { "No calendar accepts new events." }
  pending.forEach { entry ->
    val event = EKEvent.eventWithEventStore(store).apply {
      this.calendar = calendar
      title = entry.title
      notes = entry.description
      startDate = NSDate.dateWithTimeIntervalSince1970(entry.start.toEpochMilliseconds() / 1000.0)
      endDate = NSDate.dateWithTimeIntervalSince1970(entry.end.toEpochMilliseconds() / 1000.0)
      entry.url?.let { URL = NSURL.URLWithString(it) }
    }
    check(store.saveEvent(event, span = EKSpan.EKSpanThisEvent, commit = false, error = null)) {
      "Unable to prepare the calendar event."
    }
  }
  check(store.commit(null)) { "Unable to save the calendar events." }
  defaults.setObject((saved + pending.map { it.savedKey }).toList(), forKey = SAVED_ENTRIES_KEY)
  return CalendarExportResult.Added
}

// A rescheduled match gets a new key, so its new time is added on the next export.
private val CalendarEntry.savedKey: String
  get() = "$uid/${start.epochSeconds}"

private const val SAVED_ENTRIES_KEY = "calendar.savedEntries"

private class CalendarFilePreview : NSObject(), UIDocumentInteractionControllerDelegateProtocol {
  // UIKit does not retain the interaction controller while its preview is on screen.
  private var interaction: UIDocumentInteractionController? = null
  private var presenter: UIViewController? = null

  fun present(fileName: String, content: String, controller: UIViewController) {
    val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
    val bytes = content.encodeToByteArray()
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    check(data.writeToURL(url, atomically = true)) { "Unable to prepare the calendar file." }
    var presenter = controller
    while (presenter.presentedViewController != null) presenter = presenter.presentedViewController!!
    this.presenter = presenter
    val interaction = UIDocumentInteractionController.interactionControllerWithURL(url)
    interaction.delegate = this
    this.interaction = interaction
    check(interaction.presentPreviewAnimated(true)) { "Unable to preview the calendar file." }
  }

  override fun documentInteractionControllerViewControllerForPreview(
    controller: UIDocumentInteractionController,
  ): UIViewController = presenter!!

  override fun documentInteractionControllerDidEndPreview(controller: UIDocumentInteractionController) {
    interaction = null
    presenter = null
  }
}
