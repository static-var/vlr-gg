/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.data.mapper

import dev.staticvar.vlr.domain.model.CurrentMatchMap
import dev.staticvar.vlr.remotesource.common.MatchStatus
import dev.staticvar.vlr.remotesource.match.CurrentMapDto
import dev.staticvar.vlr.remotesource.match.MatchPreviewDto
import dev.staticvar.vlr.remotesource.match.TeamDto
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchOverviewMapperTest {
  private val bind = CurrentMapDto(name = "Bind", number = 2, scores = listOf(7, 5))

  private fun preview(status: MatchStatus?, currentMap: CurrentMapDto? = bind) = MatchPreviewDto(
    id = "m1",
    status = status,
    team1 = TeamDto(name = "Fnatic", score = 1),
    team2 = TeamDto(name = "Sentinels", score = 0),
    currentMap = currentMap,
  )

  private fun MatchPreviewDto.listMap() = toOverviewEntity().toDomain().currentMap

  @Test
  fun `live list entry keeps its current map through storage`() {
    assertEquals(CurrentMatchMap("Bind", 2, 7, 5), preview(MatchStatus.LIVE).listMap())
    assertEquals(CurrentMatchMap("Bind", 2, 7, 5), preview(MatchStatus.ONGOING).listMap())
  }

  @Test
  fun `list entry that is not live drops its current map`() {
    assertEquals(null, preview(MatchStatus.COMPLETED).listMap())
    assertEquals(null, preview(MatchStatus.UPCOMING).listMap())
    assertEquals(null, preview(status = null).listMap())
  }

  @Test
  fun `list map follows the shared parser`() {
    assertEquals(null, preview(MatchStatus.LIVE, currentMap = null).listMap())
    assertEquals(null, preview(MatchStatus.LIVE, bind.copy(name = "")).listMap())
    assertEquals(CurrentMatchMap("Bind", null, 7, 5), preview(MatchStatus.LIVE, bind.copy(number = 0)).listMap())
    assertEquals(CurrentMatchMap("Bind", 2, 7, null), preview(MatchStatus.LIVE, bind.copy(scores = listOf(7))).listMap())
  }
}
