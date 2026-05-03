/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.match.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.vlr.domain.model.MatchPreview
import dev.staticvar.vlr.domain.model.MatchStatus

@Composable
public fun MatchPreviewItem(modifier: Modifier = Modifier, matchPreview: MatchPreview) {
  PrismCard(modifier = modifier, style = PrismCardStyle.Filled) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      PrismHeader(text = matchPreview.event)
      when (matchPreview.status) {
        MatchStatus.UPCOMING -> PrismTag(text = matchPreview.time ?: "")
        MatchStatus.LIVE -> PrismTag(text = "LIVE", style = PrismTagStyle.Danger)
        MatchStatus.COMPLETED -> PrismTag(text = matchPreview.time ?: "")
        MatchStatus.UNKNOWN -> {}
      }
    }
  }
}
