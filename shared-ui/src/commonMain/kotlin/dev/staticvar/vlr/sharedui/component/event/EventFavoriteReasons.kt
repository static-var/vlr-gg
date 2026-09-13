/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventFavoriteReason
import dev.staticvar.vlr.domain.model.EventFavoriteSource
import dev.staticvar.vlr.sharedui.component.common.FavoriteReasonTags

/** Shows the saved items that connect an event to the personalized feed. */
@Composable
public fun EventFavoriteReasons(reasons: List<EventFavoriteReason>, modifier: Modifier = Modifier) {
  val labels = remember(reasons) {
    reasons.map(EventFavoriteReason::source).distinct().sortedBy(EventFavoriteSource::ordinal).map { source ->
      when (source) {
        EventFavoriteSource.MATCH -> "Match"
        EventFavoriteSource.TEAM -> "Team"
        EventFavoriteSource.PLAYER -> "Player"
        EventFavoriteSource.EVENT -> "Event"
      }
    }
  }
  FavoriteReasonTags(
    labels = labels,
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs, Alignment.End),
  )
}
