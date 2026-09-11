/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.staticvar.vlr.featurematches.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.darwin.NSObject
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKitUI.EKEventEditViewController
import platform.EventKitUI.EKEventEditViewDelegateProtocol
import platform.EventKitUI.EKEventEditViewAction
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

@Composable
public actual fun rememberMatchCalendarExporter(): (MatchCalendarEvent) -> Result<Unit> {
  val controller = LocalUIViewController.current
  val delegate = remember { CalendarEditorDelegate() }
  return remember(controller, delegate) {
    { event ->
      openCalendarWithIcsFallback(
        openCalendar = {
          check(NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion >= 17 })
          val store = EKEventStore()
          val calendarEvent = EKEvent.eventWithEventStore(store).apply {
            title = event.title
            notes = event.description
            startDate = NSDate.dateWithTimeIntervalSince1970(event.start.toEpochMilliseconds() / 1000.0)
            endDate = NSDate.dateWithTimeIntervalSince1970(event.end.toEpochMilliseconds() / 1000.0)
            if (event.matchId.isNotEmpty() && event.matchId.all(Char::isDigit)) {
              URL = NSURL.URLWithString("https://www.vlr.gg/${event.matchId}")
            }
          }
          val editor = EKEventEditViewController().apply {
            eventStore = store
            this.event = calendarEvent
            editViewDelegate = delegate
          }
          controller.calendarPresenter().presentViewController(editor, animated = true, completion = null)
        },
        shareIcs = {
          val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + event.fileName)
          val bytes = event.toICalendar().encodeToByteArray()
          val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
          check(data.writeToURL(url, atomically = true)) { "Unable to prepare the calendar file." }
          val presenter = controller.calendarPresenter()
          val activity = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
          activity.popoverPresentationController?.apply {
            sourceView = presenter.view
            sourceRect = presenter.view.bounds
            permittedArrowDirections = 0uL
          }
          presenter.presentViewController(activity, animated = true, completion = null)
        },
      )
    }
  }
}

private fun UIViewController.calendarPresenter(): UIViewController {
  var presenter = this
  while (presenter.presentedViewController != null) presenter = presenter.presentedViewController!!
  return presenter
}

private class CalendarEditorDelegate : NSObject(), EKEventEditViewDelegateProtocol {
  override fun eventEditViewController(
    controller: EKEventEditViewController,
    didCompleteWithAction: EKEventEditViewAction,
  ) {
    controller.dismissViewControllerAnimated(true, completion = null)
  }
}
