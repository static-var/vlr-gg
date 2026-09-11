/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.calendar

import android.content.ClipData
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
public actual fun rememberMatchCalendarExporter(): (MatchCalendarEvent) -> Result<Unit> {
  val context = LocalContext.current
  return remember(context) {
    { event ->
      openCalendarWithIcsFallback(
        openCalendar = {
          val insert = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, event.title)
            putExtra(CalendarContract.Events.DESCRIPTION, event.description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.start.toEpochMilliseconds())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.toEpochMilliseconds())
          }
          context.startActivity(insert)
        },
        shareIcs = {
          val directory = File(context.cacheDir, "calendar").apply { mkdirs() }
          val file = File(directory, event.fileName).apply { writeText(event.toICalendar(), Charsets.UTF_8) }
          val uri = FileProvider.getUriForFile(context, "${context.packageName}.calendar", file)
          val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Match calendar", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(share, "Export calendar file"))
        },
      )
    }
  }
}
