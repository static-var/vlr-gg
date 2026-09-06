/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import dev.staticvar.vlr.domain.model.ArticleBlock
import dev.staticvar.vlr.domain.model.ArticleLink
import dev.staticvar.vlr.domain.model.ArticleTextRun
import dev.staticvar.vlr.domain.model.NewsArticle
import dev.staticvar.vlr.domain.model.NewsArticleMedia
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NewsDetailContentTest {
  @Test
  fun structuredBlocksKeepMediaCaptionsAndInterviewFormattingInDocumentOrder() {
    val article = article("Unused legacy text").copy(
      blocks = listOf(
        ArticleBlock("paragraph", runs = listOf(ArticleTextRun("Intro", italic = true))),
        ArticleBlock("image", url = "//example.com/photo.jpg", alt = "Team"),
        ArticleBlock("caption", runs = listOf(ArticleTextRun("Credit"))),
        ArticleBlock("heading", level = 2, runs = listOf(ArticleTextRun("Interview"))),
        ArticleBlock(
          "blockquote",
          children = listOf(
            ArticleBlock(
              "paragraph",
              runs = listOf(ArticleTextRun("Question", "/123", bold = true)),
            ),
          ),
        ),
        ArticleBlock(
          "list",
          ordered = true,
          start = 3,
          children = listOf(
            ArticleBlock(
              "list_item",
              children = listOf(
                ArticleBlock(
                  "paragraph",
                  runs = listOf(ArticleTextRun("Answer")),
                ),
              ),
            ),
          ),
        ),
        ArticleBlock("video", url = "https://example.com/video"),
      ),
    )
    assertEquals(
      listOf(
        NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Intro", italic = true))),
        NewsDetailContentBlock.Image("https://example.com/photo.jpg", "Team"),
        NewsDetailContentBlock.Caption(listOf(NewsDetailTextRun("Credit"))),
        NewsDetailContentBlock.Heading(listOf(NewsDetailTextRun("Interview")), 2),
        NewsDetailContentBlock.Quote(
          listOf(
            NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Question", "https://www.vlr.gg/123", bold = true))),
          ),
        ),
        NewsDetailContentBlock.ListBlock(
          listOf(
            NewsDetailContentBlock.ListItem(listOf(NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Answer"))))),
          ),
          true,
          3,
        ),
        NewsDetailContentBlock.Video("https://example.com/video"),
      ),
      newsDetailContentBlocks(article),
    )
  }

  @Test
  fun renders_the_api_references_in_content_order_including_repeated_links() {
    val article = article(
      "Opening {{link_1}}, then {{link_0}}.\n\n{image_0}\n\nAfter the image.\n\n{video_0}\n\n{{link_1}} again.",
    )
    assertEquals(
      listOf(
        NewsDetailContentBlock.Text(
          listOf(
            NewsDetailTextRun("Opening "),
            NewsDetailTextRun("Second", "https://example.com/second"),
            NewsDetailTextRun(", then "),
            NewsDetailTextRun("First", "https://example.com/first"),
            NewsDetailTextRun("."),
          ),
        ),
        NewsDetailContentBlock.Image("https://example.com/photo.jpg"),
        NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("After the image."))),
        NewsDetailContentBlock.Video("https://example.com/video"),
        NewsDetailContentBlock.Text(
          listOf(
            NewsDetailTextRun("Second", "https://example.com/second"),
            NewsDetailTextRun(" again."),
          ),
        ),
      ),
      newsDetailContentBlocks(article),
    )
  }

  @Test
  fun preserves_server_text_lists_line_breaks_and_literal_markup() {
    val text = "- First\n- Second\n1. Third\nLiteral <em>text</em> & content."
    assertEquals(
      listOf(NewsDetailContentBlock.Text(listOf(NewsDetailTextRun(text)))),
      newsDetailContentBlocks(article(text)),
    )
  }

  @Test
  fun invalid_reference_indices_do_not_crash_or_shift_other_references() {
    val blocks = newsDetailContentBlocks(article("{{link_99}}\n\n{image_999999999999999}\n\n{{link_0}}"))
    assertEquals(
      listOf(
        NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Link unavailable"))),
        NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Image unavailable"))),
        NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("First", "https://example.com/first"))),
      ),
      blocks,
    )
  }

  @Test
  fun only_web_destinations_are_opened() {
    assertNull("javascript:alert(1)".newsDetailWebUrl())
    assertEquals("https://owcdn.net/photo.jpg", "//owcdn.net/photo.jpg".newsDetailWebUrl())
  }

  private fun article(content: String) = NewsArticle(
    id = "750541",
    url = "https://www.vlr.gg/750541",
    title = "Article",
    author = "Author",
    date = "2026-09-05",
    coverUrl = "",
    contentHtml = content,
    media = NewsArticleMedia(
      links = listOf(
        ArticleLink("First", "https://example.com/first"),
        ArticleLink("Second", "https://example.com/second"),
      ),
      images = listOf("https://example.com/photo.jpg"),
      videos = listOf("https://example.com/video"),
    ),
  )
}
