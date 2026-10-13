/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.icon.PrismIcon
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventPrize
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.match_event_tbd

/**
 * Compact prize tile for horizontal event prize rails.
 *
 * Mirrors [EventDetailTeamItem]: placement on top, then the team that earned it and the prize
 * amount. Unawarded placements show a trophy placeholder and TBD.
 */
@Composable
public fun EventDetailPrizeTile(prize: EventPrize, modifier: Modifier = Modifier, onTeamClick: (() -> Unit)? = null) {
  val team = prize.team
  val teamName = team?.name ?: stringResource(Res.string.match_event_tbd)
  PrismCard(modifier = modifier, style = PrismCardStyle.Filled, onClick = onTeamClick) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs, Alignment.CenterVertically),
    ) {
      PrismTag(
        text = prize.position,
        modifier = Modifier.padding(bottom = Prism.dimens.spacingXs),
        style = if (team != null) PrismTagStyle.Accent else PrismTagStyle.Neutral,
      )
      if (team != null) {
        SharedNetworkIcon(
          imageUrl = team.logoUrl,
          contentDescription = teamName,
          size = PrismIconSize.Large,
          style = PrismIconStyle.Borderless,
          tint = PrismIconTint.None,
        )
      } else {
        PrismIcon(
          imageVector = Prism.icons.events.unselected,
          contentDescription = null,
          size = PrismIconSize.Large,
          style = PrismIconStyle.Borderless,
          tint = PrismIconTint.Alt,
        )
      }
      Text(
        text = teamName,
        style = Prism.typography.bodySmall,
        color = if (team != null) Prism.color.titleColor else Prism.color.labelColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = prize.prize,
        style = Prism.typography.numericSecondary,
        color = Prism.color.titleColor,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
