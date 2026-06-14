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
import androidx.compose.ui.text.style.TextOverflow
import dev.staticvar.designsystem.component.section.PrismSectionTitle
import dev.staticvar.designsystem.component.surface.PrismSurface
import dev.staticvar.designsystem.component.tag.PrismTag
import dev.staticvar.designsystem.component.tag.PrismTagStyle
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticle

/**
 * Article body section that renders sanitized article HTML as readable paragraphs.
 */
@Composable
public fun NewsDetailStoryItem(article: NewsArticle, modifier: Modifier = Modifier) {
  val blocks = remember(article.contentHtml) {
    newsDetailArticleBlocks(contentHtml = article.contentHtml)
  }

  if (blocks.isEmpty()) return

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    PrismSectionTitle(title = "Story", preLabel = "content")
    Column(verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS)) {
      blocks.forEach { block ->
        when (block) {
          is NewsDetailArticleBlock.Image -> NewsDetailMediaBlock(
            type = "Image",
            title = block.description.ifBlank { "Article image" },
            url = block.url,
          )

          is NewsDetailArticleBlock.ListItem -> NewsDetailListItem(text = block.text)

          is NewsDetailArticleBlock.Paragraph -> NewsDetailParagraph(text = block.text)

          is NewsDetailArticleBlock.Video -> NewsDetailMediaBlock(
            type = "Video",
            title = "Embedded video",
            url = block.url,
          )
        }
      }
    }
  }
}

@Composable
private fun NewsDetailParagraph(text: String) {
  Text(
    text = text,
    style = Prism.typography.bodySmall,
    color = Prism.color.bodyColor,
  )
}

@Composable
private fun NewsDetailMediaBlock(type: String, title: String, url: String) {
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
          text = title,
          style = Prism.typography.cardTitle,
          color = Prism.color.titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = url,
          modifier = Modifier.padding(top = Prism.dimens.spacingXs),
          style = Prism.typography.caption,
          color = Prism.color.labelColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      PrismTag(text = type, style = PrismTagStyle.Neutral)
    }
  }
}

@Composable
private fun NewsDetailListItem(text: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = Prism.dimens.spacingS),
    horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Text(
      text = "-",
      style = Prism.typography.bodySmall,
      color = Prism.color.titleColor,
    )
    Text(
      text = text,
      modifier = Modifier.weight(1f),
      style = Prism.typography.bodySmall,
      color = Prism.color.bodyColor,
    )
  }
}
