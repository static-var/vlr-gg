/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.standings

import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandingsDataSourceTest {

  @Test
  fun byYear_parses_real_standings_fixture() = runTest {
    val json = readFixture("standings_2025.json")
    val ds = StandingsDataSourceImpl(singleResponseClient(json))
    val standings = ds.byYear(2025).getOrThrow()
    assertEquals(2025, standings.year)
    assertTrue(standings.circuits.isNotEmpty(), "Circuits should not be empty")
  }
}
