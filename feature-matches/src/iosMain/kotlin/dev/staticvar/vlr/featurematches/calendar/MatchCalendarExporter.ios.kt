/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featurematches.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.staticvar.vlr.sharedui.calendar.CalendarExportResult
import dev.staticvar.vlr.sharedui.calendar.matchCalendarEntry
import dev.staticvar.vlr.sharedui.calendar.rememberCalendarExporter

@Composable
public actual fun rememberMatchCalendarExporter(): suspend (MatchCalendarEvent) -> CalendarExportResult {
  val export = rememberCalendarExporter()
  return remember(export) {
    { event ->
      export(event.fileName, listOf(matchCalendarEntry(event.matchId, event.title, event.description, event.start)))
    }
  }
}
