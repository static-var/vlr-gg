/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIcon
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconSize
import dev.staticvar.designsystem.component.favorite.PrismFavoriteIconStyle
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant

/**
 * Host wrapper for cards that place the favorite marker as a ticket over the card edge.
 */
@Composable
internal fun FavoriteTicketCardBox(
  selected: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val ticketGutter = Prism.dimens.spacingS + Prism.dimens.spacingXs

  Box(modifier = modifier.padding(top = ticketGutter)) {
    content()
    if (selected) {
      PrismFavoriteIcon(
        selected = true,
        size = PrismFavoriteIconSize.Medium,
        style = PrismFavoriteIconStyle.Bare,
        modifier = Modifier
          .align(Alignment.TopStart)
          .offset(x = Prism.dimens.spacingS, y = -ticketGutter)
          .zIndex(1f),
      )
    }
  }
}

@PrismPreview
@Composable
internal fun FavoriteTicketCardBoxPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(Prism.dimens.spacingM),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingM),
    ) {
      FavoriteTicketCardBox(selected = false) {
        FavoriteTicketPreviewCard(style = PrismCardStyle.Outlined, title = "Default card")
      }
      FavoriteTicketCardBox(selected = true) {
        FavoriteTicketPreviewCard(style = PrismCardStyle.Filled, title = "Favorite ticket")
      }
      FavoriteTicketCardBox(selected = false) {
        FavoriteTicketPreviewCard(style = PrismCardStyle.Filled, title = "Default card")
      }
    }
  }
}

@Composable
private fun FavoriteTicketPreviewCard(style: PrismCardStyle, title: String) {
  PrismCard(modifier = Modifier.fillMaxWidth(), style = style) {
    Text(text = "// Global", style = Prism.typography.labelAlt, color = Prism.color.labelColor)
    Text(
      text = title,
      modifier = Modifier.padding(top = Prism.dimens.spacingS),
      style = Prism.typography.cardTitle,
      color = Prism.color.titleColor,
    )
    Text(
      text = "Ticket marker is hosted above the card edge.",
      modifier = Modifier.padding(top = Prism.dimens.spacingXs),
      style = Prism.typography.bodySmall,
      color = Prism.color.bodyColor,
    )
  }
}
