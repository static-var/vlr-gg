/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.event.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.EventPreview
import dev.staticvar.vlr.domain.model.EventStatus
import dev.staticvar.vlr.sharedui.component.common.FavoriteTicketCardBox
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

@Composable
public fun EventPreviewItem(
  modifier: Modifier = Modifier,
  eventPreview: EventPreview,
  onClick: (() -> Unit)? = null,
) {
  FavoriteTicketCardBox(selected = eventPreview.isFavorite, modifier = modifier) {
    PrismCard(
      modifier = Modifier.fillMaxWidth(),
      style = if (eventPreview.isFavorite) PrismCardStyle.Outlined else PrismCardStyle.Filled,
      onClick = onClick,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        PrismHeader(text = eventPreview.region)
        PrismTag(text = eventPreview.status.label, style = eventPreview.status.tagStyle)
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SharedNetworkIcon(
          imageUrl = eventPreview.logoUrl,
          contentDescription = eventPreview.title,
          size = PrismIconSize.Large,
          style = PrismIconStyle.Bordered,
          tint = PrismIconTint.None,
        )
        Column(modifier = Modifier.padding(start = Prism.dimens.spacingS)) {
          Text(
            text = eventPreview.title,
            style = Prism.typography.cardTitle,
            color = Prism.color.titleColor,
          )
          Text(
            text = eventPreview.dates,
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            style = Prism.typography.label,
            color = Prism.color.bodyColor,
          )
        }
        Spacer(modifier = Modifier.weight(1f))
      }
      Text(
        text = eventPreview.prize,
        modifier = Modifier.padding(top = Prism.dimens.spacingS),
        style = Prism.typography.bodySmall,
        color = Prism.color.labelColor,
      )
    }
  }
}

private val EventStatus.label: String
  get() = when (this) {
    EventStatus.ONGOING -> "ONGOING"
    EventStatus.PAUSED -> "PAUSED"
    EventStatus.UPCOMING -> "UPCOMING"
    EventStatus.COMPLETED -> "COMPLETED"
    EventStatus.UNKNOWN -> "UNKNOWN"
  }

private val EventStatus.tagStyle: PrismTagStyle
  get() = when (this) {
    EventStatus.ONGOING -> PrismTagStyle.Danger
    EventStatus.PAUSED -> PrismTagStyle.Neutral
    EventStatus.UPCOMING -> PrismTagStyle.Info
    EventStatus.COMPLETED -> PrismTagStyle.Success
    EventStatus.UNKNOWN -> PrismTagStyle.Neutral
  }
