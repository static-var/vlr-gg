/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventStanding
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

/**
 * Standings row for event group tables.
 */
@Composable
public fun EventDetailStandingItem(standing: EventStanding, modifier: Modifier = Modifier) {
  PrismCard(modifier = modifier.fillMaxWidth(), style = PrismCardStyle.Filled) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      SharedNetworkIcon(
        imageUrl = standing.teamLogoUrl,
        contentDescription = standing.teamName,
        size = PrismIconSize.Medium,
        style = PrismIconStyle.Bordered,
        tint = PrismIconTint.Primary,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = standing.teamName,
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = listOfNotNull(standing.teamCountry, standing.groupName).filter(String::isNotBlank)
            .joinToString(separator = " • ")
            .ifBlank { "Open bracket" },
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.bodySmall,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      PrismTag(text = "${standing.wins}-${standing.losses}-${standing.ties}", style = PrismTagStyle.Info)
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = Prism.dimens.spacingS),
      horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
    ) {
      PrismTag(text = "Maps ${standing.mapDifference}", style = PrismTagStyle.Neutral)
      PrismTag(text = "Rounds ${standing.roundDifference}", style = PrismTagStyle.Neutral)
      PrismTag(text = "Delta ${standing.roundDelta}", style = PrismTagStyle.Neutral)
    }
  }
}
