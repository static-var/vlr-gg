/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventFavoriteReason
import dev.staticvar.vlr.domain.model.EventFavoriteSource
import dev.staticvar.vlr.sharedui.component.common.FavoriteReasonTags
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.format_reason_event
import vlr.shared_ui.generated.resources.format_reason_match
import vlr.shared_ui.generated.resources.format_reason_player
import vlr.shared_ui.generated.resources.format_reason_team

/** Shows the saved items that connect an event to the personalized feed. */
@Composable
public fun EventFavoriteReasons(reasons: List<EventFavoriteReason>, modifier: Modifier = Modifier) {
  val labels = run {
    reasons.map(EventFavoriteReason::source).distinct().sortedBy(EventFavoriteSource::ordinal).map { source ->
      when (source) {
        EventFavoriteSource.MATCH -> stringResource(Res.string.format_reason_match)
        EventFavoriteSource.TEAM -> stringResource(Res.string.format_reason_team)
        EventFavoriteSource.PLAYER -> stringResource(Res.string.format_reason_player)
        EventFavoriteSource.EVENT -> stringResource(Res.string.format_reason_event)
      }
    }
  }
  FavoriteReasonTags(
    labels = labels,
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs, Alignment.End),
  )
}
