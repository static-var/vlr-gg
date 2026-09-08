/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.MatchFavoriteReason
import dev.staticvar.vlr.domain.model.MatchFavoriteSource

@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MatchFavoriteReasons(reasons: List<MatchFavoriteReason>, modifier: Modifier = Modifier) {
  val labels = remember(reasons) { matchFavoriteReasonLabels(reasons) }
  if (labels.isEmpty()) return
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
  ) {
    labels.forEach { label ->
      PrismTag(
        text = label,
        style = PrismTagStyle.Accent,
        modifier = Modifier.semantics { contentDescription = "Favorite source: $label" },
      )
    }
  }
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
