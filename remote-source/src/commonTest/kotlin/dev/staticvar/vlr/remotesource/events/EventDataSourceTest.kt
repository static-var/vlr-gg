/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.events

import dev.staticvar.vlr.remotesource.common.EventStatus
import dev.staticvar.vlr.remotesource.mockClient
import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventDataSourceTest {

  @Test
  fun list_parses_real_events_fixture() = runTest {
    val json = readFixture("events.json")
    val ds = EventDataSourceImpl(singleResponseClient(json))
    val result = ds.list()
    assertTrue(result.isSuccess)
    val events = result.getOrThrow()
    assertTrue(events.isNotEmpty())
    // Validate first few mapping semantics
    val champions = events.first { it.id == "2283" }
    assertEquals(EventStatus.ONGOING, champions.status)
  }

  @Test
  fun details_parses_real_event_details_fixture() = runTest {
    val json = readFixture("event_details.json")
    val ds = EventDataSourceImpl(singleResponseClient(json))
    val result = ds.details("2283")
    assertTrue(result.isSuccess)
    val details = result.getOrThrow()
    assertEquals("2283", details.id)
    assertTrue(details.matches.isNotEmpty())
    assertTrue(details.teams.isNotEmpty())
    assertTrue(details.prizes.isNotEmpty())
  }

  @Test
  fun details_unknown_status_decodes_to_null() = runTest {
    val mutated = readFixture("event_details.json").replace("\"ongoing\"", "\"brand_new\"")
    val ds = EventDataSourceImpl(singleResponseClient(mutated))
    val details = ds.details("2283").getOrThrow()
    assertEquals(null, details.status)
  }
}
