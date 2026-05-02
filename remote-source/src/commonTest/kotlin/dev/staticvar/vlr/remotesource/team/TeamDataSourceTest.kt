/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.team

import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeamDataSourceTest {
  @Test
  fun details_parses_real_fixture() = runTest {
    val json = readFixture("team_2593.json")
    val ds = TeamDataSourceImpl(singleResponseClient(json))
    val details = ds.details("2593").getOrThrow()
    assertEquals("FNATIC", details.name)
    assertTrue(details.roster.isNotEmpty())
    assertTrue(details.completed.isNotEmpty())
    assertTrue(details.upcoming.isNotEmpty())
  }
}
