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
import dev.staticvar.designsystem.component.button.PrismButton
import dev.staticvar.designsystem.component.button.PrismButtonStyle
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventDetails
import dev.staticvar.vlr.sharedui.component.common.DetailStatItem
import dev.staticvar.vlr.sharedui.component.common.DetailStatStrip
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

/**
 * Event detail summary card for title, status, logo, core metadata, and optional actions.
 */
@Composable
public fun EventDetailHeaderItem(
  event: EventDetails,
  modifier: Modifier = Modifier,
  onOpenEvent: (() -> Unit)? = null,
  onFavoriteClick: (() -> Unit)? = null,
) {
  FavoriteTicketCardBox(selected = event.isFavorite, modifier = modifier.fillMaxWidth()) {
    PrismCard(modifier = Modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        PrismHeader(text = event.region.ifBlank { "event" })
        PrismTag(text = event.status.eventDetailLabel, style = event.status.eventDetailTagStyle)
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = Prism.dimens.spacingM),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = event.title,
            style = Prism.typography.sectionTitle,
            color = Prism.color.titleColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          if (event.subtitle.isNotBlank()) {
            Text(
              text = event.subtitle,
              modifier = Modifier.padding(top = Prism.dimens.spacingXs),
              style = Prism.typography.bodySmall,
              color = Prism.color.labelColor,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        SharedNetworkIcon(
          imageUrl = event.logoUrl,
          contentDescription = event.title,
          size = PrismIconSize.Hero,
          style = PrismIconStyle.Plain,
          tint = PrismIconTint.None,
        )
      }

      DetailStatStrip(
        items = listOf(
          DetailStatItem(value = event.eventHeroTeamsStat(), label = "Teams"),
          DetailStatItem(value = event.prize.eventHeroPrizeStat().ifBlank { "TBD" }, label = "Prize"),
          DetailStatItem(value = event.region.eventHeroRegionStat(), label = "Region"),
        ),
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
      )

      if (onOpenEvent != null || onFavoriteClick != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = Prism.dimens.spacingS),
          horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
        ) {
          if (onOpenEvent != null) {
            PrismButton(onClick = onOpenEvent, modifier = Modifier.weight(1f), style = PrismButtonStyle.Primary) {
              Text(text = "View at VLR")
            }
          }
          if (onFavoriteClick != null) {
            PrismButton(onClick = onFavoriteClick, modifier = Modifier.weight(1f), style = PrismButtonStyle.Secondary) {
              Text(text = if (event.isFavorite) "Following" else "Follow")
            }
          }
        }
      }
    }
  }
}
