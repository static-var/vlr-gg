/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.remotesource.player

import dev.staticvar.vlr.remotesource.readFixture
import dev.staticvar.vlr.remotesource.singleResponseClient
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerDataSourceTest {

  @Test
  fun details_parses_real_player_fixture() = runTest {
    val json = readFixture("player_438.json")
    val ds = PlayerDataSourceImpl(singleResponseClient(json))
    val details = ds.details("438").getOrThrow()
    assertEquals("Boaster", details.alias, "Alias should match real fixture")
    assertTrue(details.agents.isNotEmpty(), "Agents list should not be empty")
    // sanity on one stat field
    val first = details.agents.first()
    // Validate numeric stat fields present and non-negative
    assertTrue(first.k >= 0)
    assertTrue(first.d >= 0)
    assertTrue(first.a >= 0)
  }
}
