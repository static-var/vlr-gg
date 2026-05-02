/*
 * Copyright (c) 2022 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.remotesource.events.EventPrizeDto
import dev.staticvar.vlr.remotesource.events.EventPrizeTeamDto
import dev.staticvar.vlr.remotesource.events.EventTeamDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventExtrasMapperTest {

  @Test
  fun `event prize mapping with team`() {
    val dto = EventPrizeDto(
      position = "1st",
      prize = "$1000",
      team = EventPrizeTeamDto(id = "T1", name = "Alpha", img = "a.png", country = "us"),
    )
    val entity = dto.toPrizeEntity("E1")
    assertEquals("E1", entity.event_id)
    assertEquals("1st", entity.position)
    assertEquals("$1000", entity.prize)
    assertEquals("T1", entity.team_id)
    assertEquals("Alpha", entity.team_name)
    assertEquals("a.png", entity.team_logo_url)
  }

  @Test
  fun `event prize mapping without team`() {
    val dto = EventPrizeDto(position = "2nd", prize = "$500", team = null)
    val entity = dto.toPrizeEntity("E2")
    assertEquals("E2", entity.event_id)
    assertNull(entity.team_id)
    assertEquals("", entity.team_name)
  }

  @Test
  fun `event team mapping blank id`() {
    val dto = EventTeamDto(name = "Bravo", id = "", img = "b.png", seed = null)
    val entity = dto.toTeamEntity("E3")
    assertEquals("E3", entity.event_id)
    assertNull(entity.team_id)
    assertEquals("Bravo", entity.team_name)
  }

  @Test
  fun `event team mapping normal`() {
    val dto = EventTeamDto(name = "Charlie", id = "T3", img = "c.png", seed = "3")
    val entity = dto.toTeamEntity("E4")
    assertEquals("E4", entity.event_id)
    assertEquals("T3", entity.team_id)
    assertEquals("Charlie", entity.team_name)
    assertEquals("c.png", entity.team_logo_url)
  }
}
