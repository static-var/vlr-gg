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
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@Composable
public actual fun rememberMatchCalendarExporter(): (MatchCalendarEvent) -> Result<Unit> {
  val controller = LocalUIViewController.current
  return remember(controller) {
    { event ->
      runCatching {
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + event.fileName)
        val bytes = event.toICalendar().encodeToByteArray()
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        check(data.writeToURL(url, atomically = true)) { "Unable to prepare the calendar file." }
        var presenter = controller
        while (presenter.presentedViewController != null) presenter = presenter.presentedViewController!!
        val activity = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
        activity.popoverPresentationController?.apply {
          sourceView = presenter.view
          sourceRect = presenter.view.bounds
          permittedArrowDirections = 0uL
        }
        presenter.presentViewController(activity, animated = true, completion = null)
      }
    }
  }
}
