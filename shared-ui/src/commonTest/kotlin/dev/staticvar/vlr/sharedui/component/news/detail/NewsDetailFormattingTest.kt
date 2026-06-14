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
  fun articleParagraphsPreserveParagraphsLineBreaksAndListItems() {
    val html = """
      <p>Fnatic&nbsp;won &amp; advanced.</p>
      <p>Quote: &quot;clean retake&quot;</p>
      <ul><li>Lotus control</li><li>Haven response</li></ul>
    """.trimIndent()

    assertEquals(
      listOf(
        "Fnatic won & advanced.",
        "Quote: \"clean retake\"",
        "- Lotus control",
        "- Haven response",
      ),
      newsDetailArticleParagraphs(contentHtml = html),
    )
  }

  @Test
  fun articleBlocksPreserveMixedContentOrder() {
    val html = """
      <p>Opening paragraph.</p>
      <ul><li>First point</li><li>Second point</li></ul>
      <p>Closing paragraph.</p>
    """.trimIndent()

    assertEquals(
      listOf(
        NewsDetailArticleBlock.Paragraph("Opening paragraph."),
        NewsDetailArticleBlock.ListItem("First point"),
        NewsDetailArticleBlock.ListItem("Second point"),
        NewsDetailArticleBlock.Paragraph("Closing paragraph."),
      ),
      newsDetailArticleBlocks(contentHtml = html),
    )
  }

  @Test
  fun articleBlocksPreserveMediaBetweenTextBlocks() {
    val html = """
      <p>Opening paragraph.</p>
      <img src="https://vlr.gg/image.jpg" alt="Stage photo">
      <p>After image.</p>
      <iframe src="https://youtube.com/embed/clip"></iframe>
      <ul><li>Final note</li></ul>
    """.trimIndent()

    assertEquals(
      listOf(
        NewsDetailArticleBlock.Paragraph("Opening paragraph."),
        NewsDetailArticleBlock.Image(url = "https://vlr.gg/image.jpg", description = "Stage photo"),
        NewsDetailArticleBlock.Paragraph("After image."),
        NewsDetailArticleBlock.Video(url = "https://youtube.com/embed/clip"),
        NewsDetailArticleBlock.ListItem("Final note"),
      ),
      newsDetailArticleBlocks(contentHtml = html),
    )
  }

  @Test
  fun articleParagraphsReturnEmptyListForBlankContent() {
    assertEquals(emptyList(), newsDetailArticleParagraphs(contentHtml = "  "))
  }

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
