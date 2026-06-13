/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventDetails
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
          style = PrismIconStyle.Bordered,
          tint = if (event.isFavorite) PrismIconTint.Alt else PrismIconTint.Primary,
        )
      }

      EventDetailStatStrip(
        dates = event.dates,
        prize = event.prize,
        region = event.region,
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

@Composable
private fun EventDetailStatStrip(dates: String, prize: String, region: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth()) {
    EventDetailStatCell(
      value = dates.eventHeroDateStat().ifBlank { "TBD" },
      label = "Dates",
      modifier = Modifier.weight(1f),
    )
    EventDetailStatCell(
      value = prize.eventHeroPrizeStat().ifBlank { "TBD" },
      label = "Prize",
      modifier = Modifier.weight(1f),
    )
    EventDetailStatCell(
      value = region.ifBlank { "Region" },
      label = "Region",
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun EventDetailStatCell(value: String, label: String, modifier: Modifier = Modifier) {
  PrismSurface(
    modifier = modifier.heightIn(min = Prism.dimens.controlHeight),
    color = Prism.color.surface,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Box(
      modifier = Modifier
        .heightIn(min = Prism.dimens.controlHeight)
        .padding(Prism.dimens.spacingS),
      contentAlignment = Alignment.CenterStart,
    ) {
      Column {
        Text(
          text = value,
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = label,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
