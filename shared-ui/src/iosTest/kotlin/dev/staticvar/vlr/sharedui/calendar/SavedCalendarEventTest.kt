/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedCalendarEventTest {
  @Test
  fun existingRecordsRemainRemovableByTheirSavedIdentifier() {
    val saved = assertNotNull(SavedCalendarEvent.fromRecord(mapOf("id" to "event-1", "start" to "123.0")))

    assertEquals("event-1", saved.id)
    assertFalse(saved.matchesFallback("same-url-event", "calendar-1"))
    assertFalse(saved.matchesFallback(null, null))
  }

  @Test
  fun fallbackRequiresBothTheSameExternalEventAndCalendar() {
    val saved = SavedCalendarEvent("old-id", "server-event-1", "calendar-1")

    assertTrue(saved.matchesFallback("server-event-1", "calendar-1"))
    assertFalse(saved.matchesFallback("server-event-1", "another-calendar"))
    assertFalse(saved.matchesFallback("another-event", "calendar-1"))
    assertFalse(saved.matchesFallback(null, "calendar-1"))
  }

  @Test
  fun persistedRecordsRetainTheIdentifiersNeededAfterAnEventIdChanges() {
    val saved = SavedCalendarEvent("old-id", "server-event-1", "calendar-1")

    assertEquals(saved, SavedCalendarEvent.fromRecord(saved.toRecord()))
  }

  @Test
  fun malformedRecordsDoNotCreateOwnership() {
    assertNull(SavedCalendarEvent.fromRecord("event-1"))
    assertNull(SavedCalendarEvent.fromRecord(mapOf("id" to " ")))
    assertNull(SavedCalendarEvent.fromRecord(mapOf("externalId" to "server-event-1")))
    val saved = assertNotNull(SavedCalendarEvent.fromRecord(mapOf("id" to "event-1", "externalId" to " ", "calendarId" to 5)))
    assertFalse(saved.matchesFallback(" ", null))
  }
}
