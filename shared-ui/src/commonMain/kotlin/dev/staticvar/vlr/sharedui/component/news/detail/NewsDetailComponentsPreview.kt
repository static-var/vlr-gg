/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

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
import dev.staticvar.vlr.domain.model.ArticleLink
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsArticleMedia

@PrismPreview
@Composable
internal fun NewsDetailComponentsPreview(@PreviewParameter(PrismPreviewProvider::class) variant: PrismVariant) {
  PrismTheme(variant = variant) {
    val article = sampleNewsArticle()

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Prism.color.background)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      NewsDetailHeaderItem(article = article)
      NewsDetailStoryItem(article = article)
    }
  }
}

private fun sampleNewsArticle(): NewsArticle = NewsArticle(
  id = "article-1",
  url = "https://vlr.gg/news/example",
  title = "Masters Toronto playoff bracket locks after a tense final Swiss round",
  author = "VLR.gg",
  date = "2h ago",
  coverUrl = "",
  contentHtml = """
    The final Swiss round settled the playoff field after three close maps and a late defensive stand.

    Teams now enter a compact knockout schedule. See the {{link_0}} and {{link_1}}.

    - FNATIC secured the top seed.
    - Sentinels stayed alive with a narrow decider.

    The next round opens with rematches across both sides of the bracket.

    {image_0}

    {video_0}
  """.trimIndent(),
  media = NewsArticleMedia(
    links = listOf(
      ArticleLink(text = "Full bracket", url = "https://vlr.gg/event/bracket"),
      ArticleLink(text = "Match archive", url = "https://vlr.gg/matches"),
    ),
    images = listOf("https://owcdn.net/img/6a9b276b7e86b.jpg"),
    videos = listOf("https://www.youtube.com/watch?v=vbBd_Hu6o2M"),
  ),
)
