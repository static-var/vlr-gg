/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

/**
 * Prize placement row with optional team winner information.
 */
@Composable
public fun EventDetailPrizeItem(prize: EventPrize, modifier: Modifier = Modifier, onTeamClick: (() -> Unit)? = null) {
  PrismCard(modifier = modifier.fillMaxWidth(), style = PrismCardStyle.Filled, onClick = onTeamClick) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Text(
        text = prize.position,
        style = Prism.typography.headline,
        color = Prism.color.accent,
      )
      Text(
        text = prize.prize,
        modifier = Modifier.weight(1f),
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    val team = prize.team
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = Prism.dimens.spacingS),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      if (team != null) {
        SharedNetworkIcon(
          imageUrl = team.logoUrl,
          contentDescription = team.name,
          size = PrismIconSize.Medium,
          style = PrismIconStyle.Bordered,
          tint = PrismIconTint.Primary,
        )
      }
      Text(
        text = team?.name ?: "TBD",
        modifier = Modifier.weight(1f),
        style = Prism.typography.bodySmall,
        color = if (onTeamClick != null) Prism.color.accent else Prism.color.bodyColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
