/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.localsource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import dev.staticvar.vlr.localsource.database.DatabaseDispatchers
import dev.staticvar.vlr.localsource.database.Events
import dev.staticvar.vlr.localsource.database.Matches
import dev.staticvar.vlr.localsource.database.VlrDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventQueriesTest {
  private lateinit var driver: SqlDriver
  private lateinit var database: VlrDatabase

  @BeforeTest
  fun setup() {
    driver = inMemoryDriver(VlrDatabase.Schema)

    database = VlrDatabase(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
  }

  @AfterTest
  fun teardown() {
    driver.close()
  }

  @Test
  fun `insert and query event`() {
    val event = createTestEvent("event1")
    database.eventsQueries.insertEvent(event)
    val result = database.eventsQueries.getEventWithFavoriteStatus("event1").executeAsOne()
    assertEquals("event1", result.id)
    assertEquals(0L, result.is_favorite)
  }

  @Test
  fun `query all events empty initially`() {
    val events = database.eventsQueries.getEventsWithFavoriteStatus().executeAsList()
    assertTrue(events.isEmpty())
  }

  @Test
  fun `insert multiple events returns all`() {
    database.eventsQueries.insertEvent(createTestEvent("event1", name = "Alpha"))
    database.eventsQueries.insertEvent(createTestEvent("event2", name = "Beta"))
    val events = database.eventsQueries.getEventsWithFavoriteStatus().executeAsList()
    assertEquals(2, events.size)
  }

  @Test
  fun `upsert event updates`() {
    val event = createTestEvent("event1", name = "Alpha")
    database.eventsQueries.insertEvent(event)
    val updated = event.copy(name = "Alpha Updated")
    database.eventsQueries.insertEvent(updated)
    val result = database.eventsQueries.getEventWithFavoriteStatus("event1").executeAsOne()
    assertEquals("Alpha Updated", result.name)
  }

  @Test
  fun `delete event removes it and cascades children`() {
    val event = createTestEvent("event1")
    database.eventsQueries.insertEvent(event)
    // child data
    database.eventsQueries.insertEventPrize(
      event_id = "event1",
      position = "1st",
      prize = "$10000",
      team_id = null,
      team_name = "Winners",
      team_logo_url = "logo.png",
      team_country = "US",
    )
    database.eventsQueries.insertEventTeam(
      event_id = "event1",
      team_id = "team1",
      team_name = "Team A",
      team_logo_url = "t1.png",
      seed = "1",
    )
    database.eventsQueries.insertEventStanding(
      event_id = "event1", team_name = "Team A", team_logo_url = "t1.png", team_country = "US",
      group_name = "Group A", wins = 2, losses = 0, ties = 0, map_difference = 10,
      round_difference = 20, round_delta = 15,
    )
    // need a match first to link event_match
    // minimal match record using matchesQueries
    database.matchesQueries.insertMatch(
      Matches(
        id = "match1", event_id = "event1", event_name = event.name, event_logo_url = "logo.png",
        series = "Bo3", stage = "Stage1", status = "upcoming", time = "2025-01-01T00:00:00Z",
        eta = null, note = "", patch = null, team1_id = "t1", team1_name = "Team A",
        team1_logo_url = "t1.png", team1_score = null, team2_id = "t2", team2_name = "Team B",
        team2_logo_url = "t2.png", team2_score = null, map_count = 0, last_updated = 0,
      ),
    )
    database.eventsQueries.insertEventMatch(
      event_id = "event1",
      match_id = "match1",
      round = "R1",
      stage = "Stage1",
    )
    // delete event
    database.eventsQueries.deleteEventById("event1")
    // verify event gone
    val ev = database.eventsQueries.getEventWithFavoriteStatus("event1").executeAsOneOrNull()
    assertEquals(null, ev)
    // verify child tables empty
    assertTrue(database.eventsQueries.getEventPrizes("event1").executeAsList().isEmpty())
    assertTrue(database.eventsQueries.getEventTeams("event1").executeAsList().isEmpty())
    assertTrue(database.eventsQueries.getEventStandings("event1").executeAsList().isEmpty())
    assertTrue(database.eventsQueries.getEventMatches("event1").executeAsList().isEmpty())
  }

  @Test
  fun `favorites add and remove`() {
    val event = createTestEvent("event1")
    database.eventsQueries.insertEvent(event)
    database.eventsQueries.addFavoriteEvent("event1")
    val fav = database.eventsQueries.getEventWithFavoriteStatus("event1").executeAsOne()
    assertEquals(1L, fav.is_favorite)
    database.eventsQueries.removeFavoriteEvent("event1")
    val notFav = database.eventsQueries.getEventWithFavoriteStatus("event1").executeAsOne()
    assertEquals(0L, notFav.is_favorite)
  }

  @Test
  fun `is favorite event returns correct count`() {
    val event = createTestEvent("event1")
    database.eventsQueries.insertEvent(event)
    val count0 = database.eventsQueries.isFavoriteEvent("event1").executeAsOne()
    assertEquals(0, count0)
    database.eventsQueries.addFavoriteEvent("event1")
    val count1 = database.eventsQueries.isFavoriteEvent("event1").executeAsOne()
    assertEquals(1, count1)
  }

  @Test
  fun `get all favorite events returns only favorites`() {
    database.eventsQueries.insertEvent(createTestEvent("event1"))
    database.eventsQueries.insertEvent(createTestEvent("event2"))
    database.eventsQueries.insertEvent(createTestEvent("event3"))
    database.eventsQueries.addFavoriteEvent("event1")
    database.eventsQueries.addFavoriteEvent("event3")
    val favs = database.eventsQueries.getAllFavoriteEvents().executeAsList()
    assertEquals(2, favs.size)
    assertTrue(favs.all { it.is_favorite == 1L })
  }

  @Test
  fun `filter events by status`() {
    database.eventsQueries.insertEvent(createTestEvent("event1", status = "running"))
    database.eventsQueries.insertEvent(createTestEvent("event2", status = "upcoming"))
    database.eventsQueries.insertEvent(createTestEvent("event3", status = "running"))
    val running = database.eventsQueries.getEventsByStatus("running").executeAsList()
    assertEquals(2, running.size)
    assertTrue(running.all { it.status == "running" })
  }

  @Test
  fun `filter events by region`() {
    database.eventsQueries.insertEvent(createTestEvent("event1", region = "NA"))
    database.eventsQueries.insertEvent(createTestEvent("event2", region = "EU"))
    database.eventsQueries.insertEvent(createTestEvent("event3", region = "NA"))
    val na = database.eventsQueries.getEventsByRegion("NA").executeAsList()
    assertEquals(2, na.size)
    assertTrue(na.all { it.region == "NA" })
  }

  @Test
  fun `flow emits and updates`() = runTest {
    database.eventsQueries.insertEvent(createTestEvent("event1"))
    val flow = database.eventsQueries.getEventsWithFavoriteStatus().asFlow().mapToList(DatabaseDispatchers.database)
    val initial = flow.first()
    assertEquals(1, initial.size)
  }

  @Test
  fun `transaction rollback on error`() {
    val event = createTestEvent("event1")
    try {
      database.transaction {
        database.eventsQueries.insertEvent(event)
        throw RuntimeException("boom")
      }
    } catch (_: RuntimeException) {}
    val res = database.eventsQueries.getEventWithFavoriteStatus("event1").executeAsOneOrNull()
    assertEquals(null, res)
  }

  @Test
  fun `transaction commit success`() {
    val e1 = createTestEvent("event1")
    val e2 = createTestEvent("event2")
    database.transaction {
      database.eventsQueries.insertEvent(e1)
      database.eventsQueries.insertEvent(e2)
    }
    val events = database.eventsQueries.getEventsWithFavoriteStatus().executeAsList()
    assertEquals(2, events.size)
  }

  private fun createTestEvent(
    id: String,
    name: String = "Test Event",
    status: String? = "upcoming",
    region: String? = "NA",
  ): Events = Events(
    id = id,
    name = name,
    subtitle = "Sub",
    status = status,
    prizes = "$10000",
    dates = "2025-01-01 to 2025-01-10",
    region = region,
    logo_url = "https://example.com/logo.png",
    last_updated = 0,
  )
}
