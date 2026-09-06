/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.sharedui.component.news.overview.formatNewsPreviewDate

/** Title and byline at the beginning of the article's reading flow. */
@Composable
public fun NewsDetailHeaderItem(
  article: NewsArticle,
  modifier: Modifier = Modifier,
  actions: (@Composable RowScope.() -> Unit)? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Prism.dimens.spacingS),
  ) {
    Text(
      text = article.title.ifBlank { "Untitled article" },
      style = Prism.typography.headline,
      color = Prism.color.titleColor,
    )
    Text(
      text = "By ${article.newsDetailAuthorLabel()}",
      style = Prism.typography.label,
      color = Prism.color.titleColor,
    )
    if (article.date.isNotBlank()) {
      Text(
        text = formatNewsPreviewDate(article.date),
        style = Prism.typography.caption,
        color = Prism.color.labelColor,
      )
    }
    if (actions != null) {
      Row(horizontalArrangement = Arrangement.spacedBy(Prism.dimens.spacingXs), content = actions)
    }
  }
}
