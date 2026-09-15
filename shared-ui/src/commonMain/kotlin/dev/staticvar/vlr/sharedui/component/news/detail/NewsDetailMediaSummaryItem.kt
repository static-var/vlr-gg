/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import org.jetbrains.compose.resources.stringResource
import vlr.shared_ui.generated.resources.Res
import vlr.shared_ui.generated.resources.news_article_assets
import vlr.shared_ui.generated.resources.news_assets
import vlr.shared_ui.generated.resources.news_assets_description
import vlr.shared_ui.generated.resources.news_media

/**
 * Compact media availability summary for article detail pages.
 */
@Composable
public fun NewsDetailMediaSummaryItem(media: NewsArticleMedia, modifier: Modifier = Modifier) {
  val formattingLabels = newsMediaLabels(media)
  val labels = remember(media, formattingLabels) { media.newsDetailMediaSummaryLabels(formattingLabels) }
  if (labels.isEmpty()) return

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismSectionTitle(title = stringResource(Res.string.news_media), preLabel = stringResource(Res.string.news_assets))
    PrismSurface(
      modifier = Modifier.fillMaxWidth(),
      color = Prism.color.surfaceVariant,
      border = BorderStroke(width = Prism.dimens.strokeDefault, color = Prism.color.stroke),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(Prism.dimens.spacingS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(Res.string.news_article_assets),
            style = Prism.typography.cardTitle,
            color = Prism.color.titleColor,
          )
          Text(
            text = stringResource(Res.string.news_assets_description),
            modifier = Modifier.padding(top = Prism.dimens.spacingXs),
            style = Prism.typography.caption,
            color = Prism.color.labelColor,
          )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs)) {
          labels.forEach { label ->
            PrismTag(text = label, style = PrismTagStyle.Neutral)
          }
        }
      }
    }
  }
}
