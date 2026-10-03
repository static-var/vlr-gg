/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.calendar

import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
public actual fun rememberCalendarExporter(): suspend (fileName: String, entries: List<CalendarEntry>) -> CalendarExportResult {
  val context = LocalContext.current
  return remember(context) {
    { fileName, entries ->
      runCatching {
        val uri = withContext(Dispatchers.IO) {
          val directory = File(context.cacheDir, "calendar").apply { mkdirs() }
          val file = File(directory, fileName).apply { writeText(entries.toICalendar(), Charsets.UTF_8) }
          FileProvider.getUriForFile(context, "${context.packageName}.calendar", file)
        }
        fun Intent.grantingRead(): Intent = apply {
          clipData = ClipData.newRawUri(fileName, uri)
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // Calendar apps import every event in the file; the share targets cover devices without one.
        val open = Intent(Intent.ACTION_VIEW).setDataAndType(uri, CALENDAR_MIME_TYPE).grantingRead()
        val share = Intent(Intent.ACTION_SEND).setType(CALENDAR_MIME_TYPE).putExtra(Intent.EXTRA_STREAM, uri).grantingRead()
        val chooser = Intent.createChooser(open, null)
          .putExtra(Intent.EXTRA_ALTERNATE_INTENTS, arrayOf(share))
          .grantingRead()
        context.startActivity(chooser)
        CalendarExportResult.Opened
      }.getOrDefault(CalendarExportResult.Failed)
    }
  }
}

private const val CALENDAR_MIME_TYPE = "text/calendar"
