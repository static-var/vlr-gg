/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsItem

/** Text-only news card with article metadata, title, and description. */
@Composable
public fun NewsPreviewItem(
  newsItem: NewsItem,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  onClick: (() -> Unit)? = null,
) {
  PrismCard(
    modifier = modifier,
    style = if (selected) PrismCardStyle.Filled else PrismCardStyle.Outlined,
    onClick = onClick,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
      ) {
        PrismTag(
          text = newsItem.author.ifBlank { "unknown" },
          style = PrismTagStyle.Accent,
        )
        PrismTag(
          text = formatNewsPreviewDate(newsItem.date),
          style = PrismTagStyle.Info,
        )
      }
      Text(
        text = newsItem.title,
        style = Prism.typography.cardTitle,
        color = Prism.color.titleColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (newsItem.description.isNotBlank()) {
        Text(
          text = newsItem.description,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.bodySmall,
          color = Prism.color.bodyColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
