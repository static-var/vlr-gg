/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.staticvar.vlr.sharedui.component.common.FavoriteReasonTags
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource

@Composable
public fun MatchFavoriteReasons(reasons: List<MatchFavoriteReason>, modifier: Modifier = Modifier) {
  val labels = remember(reasons) { matchFavoriteReasonLabels(reasons) }
  FavoriteReasonTags(labels = labels, modifier = modifier)
}

internal fun matchFavoriteReasonLabels(reasons: List<MatchFavoriteReason>): List<String> = reasons
  .map(MatchFavoriteReason::source)
  .distinct()
  .sortedBy(MatchFavoriteSource::ordinal)
  .map { source ->
    when (source) {
      MatchFavoriteSource.MATCH -> "Match"
      MatchFavoriteSource.TEAM -> "Team"
      MatchFavoriteSource.PLAYER -> "Player"
      MatchFavoriteSource.EVENT -> "Event"
    }
  }
