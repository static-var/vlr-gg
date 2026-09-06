/*
 * Copyright (c) 2022-2026 Shreyansh Lodha
 * SPDX-License-Identifier: MIT
 */
package dev.staticvar.vlr.sharedui.component.news.detail

import dev.staticvar.vlr.domain.model.ArticleBlock
import dev.staticvar.vlr.domain.model.ArticleVideoPlayer
import dev.staticvar.vlr.domain.model.NewsArticle

internal data class NewsDetailTextRun(
  val text: String,
  val url: String? = null,
  val bold: Boolean = false,
  val italic: Boolean = false,
)

internal sealed interface NewsDetailContentBlock {
  data class Heading(val runs: List<NewsDetailTextRun>, val level: Int) : NewsDetailContentBlock
  data class Caption(val runs: List<NewsDetailTextRun>) : NewsDetailContentBlock
  data class Quote(val children: List<NewsDetailContentBlock>) : NewsDetailContentBlock
  data class ListBlock(val children: List<NewsDetailContentBlock>, val ordered: Boolean, val start: Int) :
    NewsDetailContentBlock
  data class ListItem(val children: List<NewsDetailContentBlock>) : NewsDetailContentBlock

  data class Text(val runs: List<NewsDetailTextRun>) : NewsDetailContentBlock

  data class Image(val url: String, val alt: String? = null) : NewsDetailContentBlock

  data class Video(val url: String, val player: ArticleVideoPlayer? = null) : NewsDetailContentBlock
}

// Article API content references the response arrays by index. Links use double braces;
// images and videos use single braces in the server's current response format.
private val ArticleReference = Regex("\\{\\{link_(\\d+)\\}\\}|\\{image_(\\d+)\\}|\\{video_(\\d+)\\}")

internal fun newsDetailContentBlocks(article: NewsArticle): List<NewsDetailContentBlock> = article.blocks
  .flatMap(::structuredContentBlocks)
  .takeIf { it.isNotEmpty() }
  ?: legacyContentBlocks(article)

private fun structuredContentBlocks(block: ArticleBlock): List<NewsDetailContentBlock> {
  val runs = block.runs.map { NewsDetailTextRun(it.text, it.url?.newsDetailWebUrl(), it.bold, it.italic) }
  val children = block.children.flatMap(::structuredContentBlocks)
  val content = if (runs.isEmpty()) children else listOf(NewsDetailContentBlock.Text(runs)) + children
  return when (block.type) {
    "paragraph" -> content

    "heading" -> listOf(NewsDetailContentBlock.Heading(runs, block.level.coerceIn(1, 6)))

    "caption" -> listOf(NewsDetailContentBlock.Caption(runs))

    "blockquote" -> listOf(NewsDetailContentBlock.Quote(content))

    "list" -> listOf(NewsDetailContentBlock.ListBlock(children, block.ordered, block.start))

    "list_item" -> listOf(NewsDetailContentBlock.ListItem(content))

    "image" -> listOf(
      block.url?.newsDetailWebUrl()?.let { NewsDetailContentBlock.Image(it, block.alt) }
        ?: NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Image unavailable"))),
    )

    "video" -> listOf(
      block.url?.newsDetailWebUrl()?.let { NewsDetailContentBlock.Video(it, block.player) }
        ?: NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Video unavailable"))),
    )

    else -> content
  }
}

private fun legacyContentBlocks(article: NewsArticle): List<NewsDetailContentBlock> = buildList {
  article.contentHtml.split("\n\n").forEach { paragraph ->
    val runs = mutableListOf<NewsDetailTextRun>()
    fun flushText() {
      if (runs.any { it.text.isNotBlank() }) add(NewsDetailContentBlock.Text(runs.toList()))
      runs.clear()
    }
    var cursor = 0
    ArticleReference.findAll(paragraph).forEach { reference ->
      if (reference.range.first > cursor) {
        runs += NewsDetailTextRun(paragraph.substring(cursor, reference.range.first))
      }
      val linkIndex = reference.groups[1]?.value?.toIntOrNull()
      val imageIndex = reference.groups[2]?.value?.toIntOrNull()
      val videoIndex = reference.groups[3]?.value?.toIntOrNull()
      when {
        reference.groups[1] != null -> {
          val link = linkIndex?.let(article.media.links::getOrNull)
          runs += NewsDetailTextRun(
            text = link?.text?.ifBlank { link.url }?.ifBlank { "Link unavailable" } ?: "Link unavailable",
            url = link?.url?.newsDetailWebUrl(),
          )
        }

        reference.groups[2] != null -> {
          flushText()
          val url = imageIndex?.let(article.media.images::getOrNull)?.newsDetailWebUrl()
          if (url != null) {
            add(NewsDetailContentBlock.Image(url))
          } else {
            add(NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Image unavailable"))))
          }
        }

        reference.groups[3] != null -> {
          flushText()
          val url = videoIndex?.let(article.media.videos::getOrNull)?.newsDetailWebUrl()
          if (url != null) {
            add(NewsDetailContentBlock.Video(url))
          } else {
            add(NewsDetailContentBlock.Text(listOf(NewsDetailTextRun("Video unavailable"))))
          }
        }
      }
      cursor = reference.range.last + 1
    }
    if (cursor < paragraph.length) runs += NewsDetailTextRun(paragraph.substring(cursor))
    flushText()
  }
}

internal fun String.newsDetailWebUrl(): String? = when {
  startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true) -> this
  startsWith("//") -> "https:$this"
  startsWith("/") -> "https://www.vlr.gg$this"
  else -> null
}
