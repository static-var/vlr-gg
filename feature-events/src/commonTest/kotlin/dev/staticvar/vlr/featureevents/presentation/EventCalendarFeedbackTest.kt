/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.featureevents.presentation

import dev.staticvar.vlr.sharedui.calendar.CalendarEntryStatus
import dev.staticvar.vlr.sharedui.calendar.CalendarExportResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import vlr.feature_events.generated.resources.Res
import vlr.feature_events.generated.resources.allow_calendar_access_to_add_matches
import vlr.feature_events.generated.resources.matches_already_in_your_calendar
import vlr.feature_events.generated.resources.matches_removed_from_your_calendar
import vlr.feature_events.generated.resources.unable_to_remove_matches_from_calendar

class EventCalendarFeedbackTest {
  @Test
  fun permissionGuidanceOnlyAppearsAfterAnAttempt() {
    assertNull(eventCalendarMessage(CalendarEntryStatus.Denied, null))
    assertEquals(
      Res.string.allow_calendar_access_to_add_matches,
      eventCalendarMessage(CalendarEntryStatus.Denied, EventCalendarFeedback(EventCalendarAction.Add, CalendarExportResult.Denied)),
    )
  }

  @Test
  fun removalFailureKeepsItsMeaningWhenStatusChanges() {
    assertEquals(
      Res.string.unable_to_remove_matches_from_calendar,
      eventCalendarMessage(CalendarEntryStatus.NotAdded, EventCalendarFeedback(EventCalendarAction.Remove, CalendarExportResult.Failed)),
    )
  }

  @Test
  fun savedMatchesExplainRemovalAndSuccessfulRemovalConfirmsIt() {
    assertEquals(Res.string.matches_already_in_your_calendar, eventCalendarMessage(CalendarEntryStatus.Added, null))
    assertEquals(
      Res.string.matches_removed_from_your_calendar,
      eventCalendarMessage(CalendarEntryStatus.NotAdded, EventCalendarFeedback(EventCalendarAction.Remove, CalendarExportResult.Removed)),
    )
  }

  @Test
  fun androidHandoffDoesNotClaimTheEventsWereSaved() {
    assertNull(
      eventCalendarMessage(CalendarEntryStatus.NotAdded, EventCalendarFeedback(EventCalendarAction.Add, CalendarExportResult.Opened)),
    )
  }
}
