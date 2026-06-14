/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.card.PrismCard
import dev.staticvar.designsystem.component.card.PrismCardStyle
import dev.staticvar.designsystem.component.header.PrismHeader
import dev.staticvar.designsystem.component.icon.PrismIconSize
import dev.staticvar.designsystem.component.icon.PrismIconStyle
import dev.staticvar.designsystem.component.icon.PrismIconTint
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.sharedui.component.common.SharedNetworkIcon

/**
 * News detail hero card for article title, source metadata, media counts, and optional actions.
 */
@Composable
public fun NewsDetailHeaderItem(
  article: NewsArticle,
  modifier: Modifier = Modifier,
  actions: (@Composable RowScope.() -> Unit)? = null,
) {
  PrismCard(modifier = modifier.fillMaxWidth(), style = PrismCardStyle.Outlined) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      PrismHeader(text = "news detail")
      PrismTag(text = "article", style = PrismTagStyle.Neutral)
    }

    if (article.coverUrl.isNotBlank()) {
      SharedNetworkIcon(
        imageUrl = article.coverUrl,
        contentDescription = article.title,
        modifier = Modifier.padding(top = Prism.dimens.spacingM),
        size = PrismIconSize.Hero,
        style = PrismIconStyle.Muted,
        contentScale = ContentScale.Crop,
        tint = PrismIconTint.None,
      )
    }

    Text(
      text = article.title.ifBlank { "Untitled article" },
      modifier = Modifier.padding(top = Prism.dimens.spacingS),
      style = Prism.typography.sectionTitle,
      color = Prism.color.titleColor,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
    )

    NewsDetailStatStrip(
      firstValue = article.newsDetailAuthorLabel(),
      firstLabel = "Author",
      secondValue = article.newsDetailDateLabel(),
      secondLabel = "Date",
      thirdValue = article.media.newsDetailAssetCountLabel(),
      thirdLabel = "Assets",
      modifier = Modifier.padding(top = Prism.dimens.spacingS),
    )

    if (actions != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = Prism.dimens.spacingS),
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs),
        content = actions,
      )
    }
  }
}

@Composable
private fun NewsDetailStatStrip(
  firstValue: String,
  firstLabel: String,
  secondValue: String,
  secondLabel: String,
  thirdValue: String,
  thirdLabel: String,
  modifier: Modifier = Modifier,
) {
  Row(modifier = modifier.fillMaxWidth()) {
    NewsDetailStatCell(value = firstValue, label = firstLabel, modifier = Modifier.weight(1f))
    NewsDetailStatCell(value = secondValue, label = secondLabel, modifier = Modifier.weight(1f))
    NewsDetailStatCell(value = thirdValue, label = thirdLabel, modifier = Modifier.weight(1f))
  }
}

@Composable
private fun NewsDetailStatCell(value: String, label: String, modifier: Modifier = Modifier) {
  PrismSurface(
    modifier = modifier.heightIn(min = Prism.dimens.controlHeight),
    color = Prism.color.surface,
    border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = Prism.dimens.controlHeight)
        .padding(Prism.dimens.spacingS),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = value,
          modifier = Modifier.fillMaxWidth(),
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = label,
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
