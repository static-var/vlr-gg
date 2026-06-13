/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventTeam
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

/**
 * Compact participant tile for horizontal event team rails.
 */
@Composable
public fun EventDetailTeamItem(team: EventTeam, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  PrismCard(modifier = modifier, style = PrismCardStyle.Filled, onClick = onClick) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    ) {
      SharedNetworkIcon(
        imageUrl = team.logoUrl,
        contentDescription = team.name,
        size = PrismIconSize.Large,
        style = PrismIconStyle.Bordered,
        tint = PrismIconTint.Primary,
      )
      Text(
        text = team.name,
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      team.seed?.takeIf(String::isNotBlank)?.let { seed ->
        PrismTag(
          text = seed,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = PrismTagStyle.Info,
        )
      }
    }
  }
}
