/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import dev.staticvar.vlr.domain.model.ArticleLink
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import kotlin.test.Test
import kotlin.test.assertEquals

class NewsDetailFormattingTest {
  @Test
  fun mediaSummaryLabelsPluralizeCounts() {
    val media = NewsArticleMedia(
      links = listOf(ArticleLink(text = "Source", url = "https://vlr.gg")),
      images = listOf("one.png", "two.png"),
      videos = listOf("clip.mp4"),
    )

    assertEquals(listOf("1 link", "2 images", "1 video"), media.newsDetailMediaSummaryLabels())
  }

  @Test
  fun mediaSummaryLabelsSkipEmptyGroups() {
    val media = NewsArticleMedia(links = emptyList(), images = emptyList(), videos = listOf("clip.mp4", "vod.mp4"))

    assertEquals(listOf("2 videos"), media.newsDetailMediaSummaryLabels())
  }

  @Test
  fun mediaAssetLabelCountsAllMediaGroups() {
    val media = NewsArticleMedia(
      links = listOf(ArticleLink(text = "Source", url = "https://vlr.gg")),
      images = listOf("one.png", "two.png"),
      videos = listOf("clip.mp4"),
    )

    assertEquals("4 assets", media.newsDetailAssetCountLabel())
  }
}
