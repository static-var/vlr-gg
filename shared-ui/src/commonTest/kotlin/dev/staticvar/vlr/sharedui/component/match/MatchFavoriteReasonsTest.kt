/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match

import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource
import kotlin.test.Test
import kotlin.test.assertEquals

class MatchFavoriteReasonsTest {
  @Test
  fun combinesDistinctSourceTagsWithoutPlayerOrTeamNames() {
    val player = MatchFavoriteReason(MatchFavoriteSource.PLAYER, "asuna", "Asuna")
    assertEquals(
      listOf("Match", "Team", "Player", "Event"),
      matchFavoriteReasonLabels(
        sourceLabels = listOf("Match", "Team", "Player", "Event"),
        reasons = listOf(
          player,
          MatchFavoriteReason(MatchFavoriteSource.EVENT, "masters", "Masters"),
          MatchFavoriteReason(MatchFavoriteSource.TEAM, "100t", "100T"),
          player,
          MatchFavoriteReason(MatchFavoriteSource.MATCH, "match-1", "100T vs Sentinels"),
          MatchFavoriteReason(MatchFavoriteSource.PLAYER, "cryo", "Cryo"),
        ),
      ),
    )
  }

  @Test
  fun showsOnlyOneTagForMultipleFavoriteTeams() {
    assertEquals(
      listOf("Team"),
      matchFavoriteReasonLabels(
        sourceLabels = listOf("Match", "Team", "Player", "Event"),
        reasons = listOf(
          MatchFavoriteReason(MatchFavoriteSource.TEAM, "100t", "100 Thieves"),
          MatchFavoriteReason(MatchFavoriteSource.TEAM, "loud", "LOUD"),
        ),
      ),
    )
  }

  @Test
  fun showsNoTagsWithoutAFavoriteSource() {
    assertEquals(
      emptyList(),
      matchFavoriteReasonLabels(sourceLabels = listOf("Match", "Team", "Player", "Event"), reasons = emptyList()),
    )
  }
}
