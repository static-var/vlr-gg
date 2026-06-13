/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import dev.staticvar.designsystem.preview.PrismPreview
import dev.staticvar.designsystem.preview.PrismPreviewProvider
import dev.staticvar.designsystem.prism.Prism
import dev.staticvar.designsystem.prism.PrismTheme
import dev.staticvar.designsystem.prism.PrismVariant
import dev.staticvar.vlr.domain.model.NewsItem

@PrismPreview
@Composable
internal fun NewsPreviewItemPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      NewsPreviewItem(newsItem = sampleNewsItem(author = "VLR.gg", selected = true), selected = true)
      NewsPreviewItem(newsItem = sampleNewsItem(author = "George Geddes"))
      NewsPreviewItem(newsItem = sampleNewsItem(author = "", description = ""))
    }
  }
}

private fun sampleNewsItem(
  author: String,
  selected: Boolean = false,
  description: String = "Roster moves, playoff implications, and the latest Valorant Champions Tour updates.",
): NewsItem = NewsItem(
  id = if (selected) "featured-story" else "story",
  url = "https://vlr.gg/news/story",
  title = if (selected) {
    "Masters Toronto playoff bracket locks after a tense final Swiss round"
  } else {
    "Pacific teams prepare for a compact stage two schedule"
  },
  description = description,
  date = "2h ago",
  author = author,
  coverUrl = "",
)
