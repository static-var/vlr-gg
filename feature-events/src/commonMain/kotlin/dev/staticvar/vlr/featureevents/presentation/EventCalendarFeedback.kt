/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.sharedui.calendar.CalendarEntryStatus
import dev.staticvar.vlr.sharedui.calendar.CalendarExportResult
import org.jetbrains.compose.resources.StringResource
import vlr.feature_events.generated.resources.Res
import vlr.feature_events.generated.resources.allow_calendar_access_to_add_matches
import vlr.feature_events.generated.resources.matches_already_in_your_calendar
import vlr.feature_events.generated.resources.matches_removed_from_your_calendar
import vlr.feature_events.generated.resources.unable_to_remove_matches_from_calendar
import vlr.feature_events.generated.resources.unable_to_save_matches_to_calendar
import vlr.feature_events.generated.resources.upcoming_matches_added_to_your_calendar

internal enum class EventCalendarAction { Add, Remove }

internal data class EventCalendarFeedback(val action: EventCalendarAction, val result: CalendarExportResult)

internal fun eventCalendarMessage(status: CalendarEntryStatus?, feedback: EventCalendarFeedback?): StringResource? =
  when (feedback?.result) {
    CalendarExportResult.Added -> Res.string.upcoming_matches_added_to_your_calendar
    CalendarExportResult.AlreadyAdded -> Res.string.matches_already_in_your_calendar
    CalendarExportResult.Removed -> Res.string.matches_removed_from_your_calendar
    CalendarExportResult.Denied -> Res.string.allow_calendar_access_to_add_matches
    CalendarExportResult.Failed -> when (feedback.action) {
      EventCalendarAction.Add -> Res.string.unable_to_save_matches_to_calendar
      EventCalendarAction.Remove -> Res.string.unable_to_remove_matches_from_calendar
    }
    CalendarExportResult.Opened -> null
    null -> Res.string.matches_already_in_your_calendar.takeIf { status == CalendarEntryStatus.Added }
  }
